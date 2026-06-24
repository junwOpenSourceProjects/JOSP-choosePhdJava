"""Main crawler logic with multi-threading support."""
import csv
import io
import logging
import re
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Any, Dict, List, Optional, Set, Tuple

from tqdm import tqdm

from client import MenggyClient
from cleaner import normalize_name, parse_rank, tag_type_by_url
from config import BASE_URL, DOWNLOAD_API_TEMPLATE, ENTITY_API_TEMPLATE
from discover import categorize_slug, discover, parse_result_tree
from storage import Storage

logger = logging.getLogger(__name__)


def _clean_detail_value(value: Any) -> Optional[str]:
    """Return a stripped string, treating empty or placeholder '.' as None."""
    if value is None:
        return None
    text = str(value).strip()
    if not text or text == ".":
        return None
    return text


class RankingCrawler:
    def __init__(self, client: MenggyClient, storage: Storage,
                 max_workers: int = 5):
        self.client = client
        self.storage = storage
        self.max_workers = max_workers
        # Shared lock to reduce cross-worker DB contention on hot rows
        self._write_lock = threading.Lock()

    def _api_entity_url(self, slug: str, year: int, page: int, t_list: str = "") -> str:
        return (
            BASE_URL
            + ENTITY_API_TEMPLATE.format(slug=slug.lstrip("/"))
            + f"?ft[year]={year}&ft[t_list]={t_list}&ft[page]={page}"
        )

    def _api_download_url(self, slug: str, year: int, t_list: str = "") -> str:
        return (
            BASE_URL
            + DOWNLOAD_API_TEMPLATE.format(slug=slug.lstrip("/"))
            + f"?ft[year]={year}&ft[t_list]={t_list}"
        )

    def _ensure_system_and_list(self, slug: str, year: int,
                                total_entities: Optional[int] = None,
                                name_zh: Optional[str] = None) -> Tuple[int, int]:
        system_code, system_name = categorize_slug(slug)
        system_slug = system_code.lower()
        system_id = self.storage.insert_system(
            slug=system_slug,
            name_zh=system_code.upper(),
            category=system_code.upper(),
        )
        list_id = self.storage.insert_list(
            system_id=system_id,
            slug=slug.lstrip("/"),
            year=year,
            name_zh=name_zh or slug.lstrip("/").replace("-", " ").title(),
            total_entities=total_entities,
            url=BASE_URL + slug,
        )
        return system_id, list_id

    def _prepare_entities(self, entities: List[dict], list_id: int, year: int,
                          data_source: str) -> Tuple[
                              List[Tuple], List[Tuple], List[Tuple], List[Tuple]
                          ]:
        """Convert API entities into bulk insert tuples."""
        universities: List[Tuple] = []
        tags: Dict[Tuple, Tuple] = {}
        uni_tags: Set[Tuple] = set()
        entries: List[Tuple] = []

        for entity in entities:
            url_id = entity.get("url_id") or str(entity.get("id"))
            name_zh = normalize_name(entity.get("name"))
            name_en = normalize_name(entity.get("eng_name"))
            name_fanti = normalize_name(entity.get("name_fanti"))
            badge_url = entity.get("badge")
            detail_url = entity.get("url") or entity.get("thread_url")

            if not url_id or not name_zh:
                logger.warning("Skipping entity without url_id/name: %s", entity)
                continue

            universities.append((url_id, name_zh, name_en, name_fanti, badge_url, detail_url))

            for tag in entity.get("tags", []):
                tag_name = tag.get("name", "")
                ttype = tag_type_by_url(tag.get("url", ""))
                tag_key = (tag_name, ttype)
                tags[tag_key] = (
                    tag.get("id"),
                    tag.get("url_id"),
                    tag_name,
                    tag.get("eng_name"),
                    ttype,
                    ",".join(map(str, tag.get("parent_id_list", [])))
                    if isinstance(tag.get("parent_id_list"), list)
                    else str(tag.get("parent_id_list", "")),
                    tag.get("icon"),
                )
                uni_tags.add((url_id, tag_name, ttype))

            rank_display = entity.get("rank", "")
            rank_raw = entity.get("rank_alias")
            rank_int, range_start, range_end, _ = parse_rank(rank_display, rank_raw)
            entries.append((
                list_id, url_id, rank_display, rank_raw, rank_int,
                range_start, range_end, year, data_source, int(bool(entity.get("liked", False)))
            ))

        return universities, list(tags.values()), list(uni_tags), entries

    def _flush_batches(self, all_universities: List[Tuple], all_tags: List[Tuple],
                       all_uni_tags: List[Tuple], all_entries: List[Tuple]):
        """Bulk insert accumulated data and refresh caches (thread-safe)."""
        with self._write_lock:
            if all_universities:
                self.storage.bulk_insert_universities(all_universities)
            if all_tags:
                self.storage.bulk_insert_tags(all_tags)
            if all_uni_tags:
                links = []
                for url_id, tag_name, ttype in all_uni_tags:
                    univ_id = self.storage.get_university_id(url_id)
                    tag_id = self.storage.get_tag_id(tag_name, ttype)
                    if univ_id and tag_id:
                        links.append((univ_id, tag_id))
                self.storage.bulk_insert_university_tags(links)
            if all_entries:
                resolved_entries = []
                for (list_id, url_id, rank_display, rank_raw, rank_int,
                     range_start, range_end, year, data_source, liked) in all_entries:
                    univ_id = self.storage.get_university_id(url_id)
                    if univ_id:
                        resolved_entries.append((
                            list_id, univ_id, rank_display, rank_raw, rank_int,
                            range_start, range_end, year, data_source, liked
                        ))
                    else:
                        logger.warning("University not found after flush: %s", url_id)
                self.storage.bulk_insert_ranking_entries(resolved_entries)

    def crawl_list(self, slug: str, year: int,
                   name_zh: Optional[str] = None) -> int:
        """Crawl a single ranking list for a given year. Returns count of entries."""
        logger.info("Crawling %s year %s", slug, year)
        self.storage.log("crawl_list", "started", identifier=f"{slug}:{year}")

        count = 0
        all_universities: List[Tuple] = []
        all_tags: List[Tuple] = []
        all_uni_tags: List[Tuple] = []
        all_entries: List[Tuple] = []

        try:
            # Ensure the list row exists up-front (we need a list_id for entries)
            _, list_id = self._ensure_system_and_list(
                slug, year, total_entities=None, name_zh=name_zh
            )

            # Crawl entity API page-by-page. This is the public, paginated source
            # and contains every university with authoritative url_id / rank.
            total: Optional[int] = None
            page = 1
            seen_keys: Set[Tuple[str, str]] = set()
            while True:
                url = self._api_entity_url(slug, year, page)
                data = self.client.get_json(url)
                entities = data.get("entities", [])
                if total is None:
                    total = data.get("total_entity") or len(entities)
                    if total:
                        self.storage.update_list_total(list_id, total)

                if not entities:
                    break

                new_entities = []
                for entity in entities:
                    key = (entity.get("url_id") or str(entity.get("id")), entity.get("rank", ""))
                    if key in seen_keys:
                        continue
                    seen_keys.add(key)
                    new_entities.append(entity)

                if not new_entities:
                    break

                unis, tags, ut, ents = self._prepare_entities(new_entities, list_id, year, "entity_api")
                all_universities.extend(unis)
                all_tags.extend(tags)
                all_uni_tags.extend(ut)
                all_entries.extend(ents)
                count += len(new_entities)

                if len(entities) < 10 or page * 10 >= (total or 0):
                    break
                page += 1

                # Periodic flush every 20 pages to keep memory bounded
                if page % 20 == 0:
                    self._flush_batches(all_universities, all_tags, all_uni_tags, all_entries)
                    all_universities, all_tags, all_uni_tags, all_entries = [], [], [], []

            # Final flush
            self._flush_batches(all_universities, all_tags, all_uni_tags, all_entries)

            self.storage.log(
                "crawl_list", "success",
                identifier=f"{slug}:{year}",
                message=f"entries={count}"
            )
        except Exception as exc:
            logger.exception("Failed to crawl %s %s: %s", slug, year, exc)
            self.storage.log(
                "crawl_list", "failed",
                identifier=f"{slug}:{year}",
                message=str(exc)
            )
            raise
        return count

    def _collect_download_rows(self, content: str, slug: str, year: int,
                               list_id: int,
                               all_universities: List[Tuple],
                               all_tags: List[Tuple],
                               all_uni_tags: List[Tuple],
                               all_entries: List[Tuple]) -> int:
        reader = csv.DictReader(io.StringIO(content), delimiter="\t")
        count = 0
        local_tags: Dict[Tuple[str, str], Tuple] = {}
        for row in reader:
            name_zh = row.get("大学名称", "").strip()
            name_en = row.get("大学英文名称", "").strip()
            tags_str = row.get("大学标签", "")
            rank_display = row.get("当前排名（取整）", "").strip()
            rank_raw = row.get("当前排名（原始数据）", "").strip()
            if not name_zh:
                continue
            url_id = self._guess_url_id(name_en) if name_en else self._guess_url_id(name_zh)
            if not url_id:
                continue
            count += 1

            all_universities.append((url_id, name_zh, name_en, None, None, f"/universities/{url_id}"))

            for tag_name in [t.strip() for t in tags_str.split(",") if t.strip()]:
                tag_key = (tag_name, "other")
                if tag_key not in local_tags:
                    local_tags[tag_key] = (None, None, tag_name, None, "other", None, None)
                all_uni_tags.append((url_id, tag_name, "other"))

            rank_int, range_start, range_end, _ = parse_rank(rank_display, rank_raw)
            all_entries.append((
                list_id, url_id, rank_display, rank_raw, rank_int,
                range_start, range_end, year, "download_api", 0
            ))
        all_tags.extend(local_tags.values())
        return count

    @staticmethod
    def _guess_url_id(name: str) -> str:
        """Heuristic to generate url_id from university name (fallback)."""
        if not name:
            return ""
        uid = re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")
        return uid

    def _crawl_one(self, target: Dict) -> Tuple[str, int, int]:
        """Worker function for one list. Returns (slug, year, count)."""
        slug = target["slug"]
        year = target["year"]
        name_zh = target.get("name_zh")
        try:
            count = self.crawl_list(slug, year, name_zh=name_zh)
            return slug, year, count
        except Exception as exc:
            logger.error("Continuing after error on %s %s: %s", slug, year, exc)
            return slug, year, -1

    def discover_and_crawl_rankings(self, limit: Optional[int] = None,
                                    skip_completed: bool = True):
        targets = discover(self.client)
        if limit:
            targets = targets[:limit]

        if skip_completed:
            completed = set()
            for row in self.storage.get_successful_list_crawls():
                parts = row["identifier"].rsplit(":", 1)
                if len(parts) == 2:
                    completed.add((parts[0], int(parts[1])))
            original_len = len(targets)
            targets = [t for t in targets if (t["slug"], t["year"]) not in completed]
            logger.info("Skipping %d already-completed lists",
                        original_len - len(targets))

        logger.info("Starting to crawl %d ranking lists with %d workers",
                    len(targets), self.max_workers)

        completed_count = 0
        failed_count = 0
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            future_to_target = {
                executor.submit(self._crawl_one, t): t for t in targets
            }
            with tqdm(total=len(targets), desc="Ranking lists") as pbar:
                for future in as_completed(future_to_target):
                    slug, year, count = future.result()
                    if count >= 0:
                        completed_count += 1
                    else:
                        failed_count += 1
                    pbar.update(1)

        logger.info("Ranking crawl finished: %d success, %d failed",
                    completed_count, failed_count)

    def fetch_website_total(self, slug: str, year: int) -> Optional[int]:
        """Fetch total_entity from the public year page (no login required)."""
        try:
            path = f"/{slug.lstrip('/')}-year/{year}"
            html = self.client.get_html(path)
            tree = parse_result_tree(html)
            if tree:
                return tree.get("thread", {}).get("total_entity")
        except Exception as exc:
            logger.warning("Failed to fetch website total for %s %s: %s", slug, year, exc)
        return None

    def audit_rankings(self, subjects_only: bool = False) -> List[Dict[str, Any]]:
        """
        Compare DB entry counts with website total_entity for every list.
        Returns a list of inconsistent/missing rows.
        """
        logger.info("Starting audit of ranking lists (subjects_only=%s)...", subjects_only)
        rows = self.storage.get_list_entry_counts()
        if subjects_only:
            rows = [r for r in rows if "subject-rankings" in r["slug"]]
        problems: List[Dict[str, Any]] = []
        skipped = 0
        for row in tqdm(rows, desc="Auditing lists"):
            slug = row["slug"]
            year = row["year"]
            db_count = row["entry_count"]
            db_total = row.get("total_entities")

            # Fast path: if we already know the expected total and the counts match,
            # there is no need to hit the website again.
            if db_total is not None and db_count == db_total:
                skipped += 1
                continue

            web_total = self.fetch_website_total(slug, year)
            if web_total is None:
                problems.append({
                    "slug": slug, "year": year, "db_count": db_count,
                    "web_total": None, "issue": "website_unreachable",
                })
                continue

            # Update stored total so future audits can skip this list
            if db_total != web_total:
                self.storage.update_list_total(row["id"], web_total)

            if db_count != web_total:
                problems.append({
                    "slug": slug, "year": year, "db_count": db_count,
                    "web_total": web_total, "issue": "count_mismatch",
                })
        logger.info("Audit complete: %d problems found, %d skipped as verified, out of %d lists",
                    len(problems), skipped, len(rows))
        return problems

    def backfill_rankings(self, problems: Optional[List[Dict[str, Any]]] = None,
                          subjects_only: bool = False):
        """
        Re-crawl lists that are missing or whose DB count differs from the website.
        If problems is None, run an audit first.
        """
        if problems is None:
            problems = self.audit_rankings(subjects_only=subjects_only)
        if not problems:
            logger.info("No backfill needed; all lists are complete.")
            return

        # Also include lists that exist in discovery but have zero entries
        empty_lists = self.storage.get_lists_without_entries()
        if subjects_only:
            empty_lists = [r for r in empty_lists if "subject-rankings" in r["slug"]]
        empty_targets = [
            {"slug": r["slug"], "year": r["year"], "name_zh": r["name_zh"]}
            for r in empty_lists
        ]

        target_map: Dict[Tuple[str, int], Dict] = {
            (p["slug"], p["year"]): {"slug": p["slug"], "year": p["year"], "name_zh": None}
            for p in problems if p.get("issue") != "website_unreachable"
        }
        for t in empty_targets:
            target_map[(t["slug"], t["year"])] = t

        targets = list(target_map.values())
        logger.info("Backfilling %d ranking lists with %d workers",
                    len(targets), self.max_workers)

        completed_count = 0
        failed_count = 0
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            future_to_target = {executor.submit(self._crawl_one, t): t for t in targets}
            with tqdm(total=len(targets), desc="Backfilling lists") as pbar:
                for future in as_completed(future_to_target):
                    slug, year, count = future.result()
                    if count >= 0:
                        completed_count += 1
                    else:
                        failed_count += 1
                    pbar.update(1)
        logger.info("Backfill finished: %d success, %d failed",
                    completed_count, failed_count)

    def crawl_university_details(self, force: bool = False):
        """Crawl detail pages for all universities in DB (multi-threaded)."""
        if force:
            rows = self.storage.get_all_university_slugs()
            logger.info("Force re-crawling %d university detail pages with %d workers",
                        len(rows), self.max_workers)
        else:
            rows = self.storage.get_university_slugs_without_detail()
            logger.info("Crawling %d university detail pages with %d workers",
                        len(rows), self.max_workers)

        def crawl_one(row: Dict) -> int:
            url_id = row["url_id"]
            detail_path = row.get("detail_url") or f"/universities/{url_id}"
            try:
                html = self.client.get_html(detail_path)
                self._parse_and_save_detail(row["id"], html)
                return 1
            except Exception as exc:
                logger.error("Failed to crawl detail %s: %s", detail_path, exc)
                self.storage.log("crawl_detail", "failed", identifier=detail_path,
                                message=str(exc))
                return 0

        completed = 0
        failed = 0
        with ThreadPoolExecutor(max_workers=self.max_workers) as executor:
            future_to_row = {executor.submit(crawl_one, r): r for r in rows}
            with tqdm(total=len(rows), desc="University details") as pbar:
                for future in as_completed(future_to_row):
                    if future.result():
                        completed += 1
                    else:
                        failed += 1
                    pbar.update(1)

        logger.info("Detail crawl finished: %d success, %d failed", completed, failed)

    def _parse_and_save_detail(self, university_id: int, html: str):
        from bs4 import BeautifulSoup

        website = location = founded_year = motto = address = description = None

        # Primary: parse the embedded result_tree JSON (authoritative structured data)
        tree = parse_result_tree(html)
        if tree:
            entity = tree.get("entity") or {}
            info_data = entity.get("info_data") or {}
            basic = info_data.get("basic") or {}
            website = _clean_detail_value(basic.get("official_url"))
            motto = _clean_detail_value(basic.get("motto"))
            founded_year = _clean_detail_value(basic.get("found_date"))
            address = _clean_detail_value(basic.get("address"))

            # Use first non-empty description from qs/the/usnews/arwu/rur blocks
            for org in ("qs", "the", "usnews", "arwu", "rur"):
                block = info_data.get(org)
                if not isinstance(block, dict):
                    continue
                uni_info = block.get("uni_info")
                if not isinstance(uni_info, dict):
                    continue
                desc = uni_info.get("description")
                if desc and len(str(desc).strip()) > 20:
                    description = str(desc).strip()
                    break

        # Fallback: scrape free text if structured data was missing
        if not description:
            soup = BeautifulSoup(html, "html.parser")
            for p in soup.find_all("p"):
                text = p.get_text(strip=True)
                if len(text) > 50:
                    description = text
                    break

        self.storage.insert_detail(
            university_id=university_id,
            page_html=html,
            description=description or None,
            website=website,
            location=location,
            founded_year=founded_year,
            motto=motto,
            address=address,
        )
