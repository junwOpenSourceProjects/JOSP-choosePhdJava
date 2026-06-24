"""Discover all ranking slugs and years that need to be crawled."""
import json
import logging
import os
import re
from typing import Dict, List, Optional, Set, Tuple

from client import MenggyClient
from config import BASE_URL, SUBJECT_CATEGORY_PAGES

logger = logging.getLogger(__name__)

SITEMAP_URL = "/sitemap.xml"
DISCOVERY_CACHE = "discovery_cache.json"


def fetch_sitemap_urls(client: MenggyClient) -> List[str]:
    """Return list of all URLs from sitemap.xml."""
    text = client.get_html(SITEMAP_URL)
    urls = re.findall(r"<loc>([^<]+)</loc>", text)
    return [u.strip() for u in urls if u.strip().startswith(BASE_URL)]


def extract_ranking_slugs(urls: List[str]) -> Dict[str, Set[str]]:
    """
    Categorize ranking URLs from sitemap.
    Returns dict with keys: base, subject.
    """
    result: Dict[str, Set[str]] = {"base": set(), "subject": set()}
    for url in urls:
        path = url.replace(BASE_URL, "")
        # base ranking overview: /xxx-rankings  (not a subject, not a detail page)
        if re.match(r"^/[a-z0-9-]+-rankings?$", path):
            result["base"].add(path)
            continue
        # specific subject ranking overview: /xxx-subject-rankings-xxx (no further /)
        if re.match(r"^/[a-z0-9-]+-subject-rankings-[a-z0-9-]+$", path):
            result["subject"].add(path)
            continue
    return result


def extract_base_years_from_sitemap(urls: List[str]) -> Dict[str, Set[int]]:
    """
    Extract available years for base rankings directly from sitemap year URLs.
    Returns {base_slug (no leading slash): {years}}.
    """
    result: Dict[str, Set[int]] = {}
    year_pattern = re.compile(r"^/([a-z0-9-]+-rankings?)-year/(\d{4})$")
    for url in urls:
        path = url.replace(BASE_URL, "")
        m = year_pattern.match(path)
        if m:
            slug = m.group(1)
            year = int(m.group(2))
            result.setdefault(slug, set()).add(year)
    return result


def parse_result_tree(html: str) -> Optional[dict]:
    """Extract the embedded result_tree JSON from a page's HTML."""
    for marker in ("const result_tree = ", "var result_tree = "):
        start = html.find(marker)
        if start < 0:
            continue
        brace_start = html.find("{", start)
        if brace_start < 0:
            continue
        balance = 0
        end = -1
        for i in range(brace_start, len(html)):
            ch = html[i]
            if ch == "{":
                balance += 1
            elif ch == "}":
                balance -= 1
                if balance == 0:
                    end = i + 1
                    break
        if end > 0:
            try:
                return json.loads(html[brace_start:end])
            except json.JSONDecodeError as exc:
                logger.warning("Failed to decode result_tree: %s", exc)
    return None


def parse_years_from_overview(html: str, base_slug: str) -> List[int]:
    """Extract available years from a ranking overview page."""
    years: Set[int] = set()

    # Authoritative source: the embedded result_tree.year_list
    tree = parse_result_tree(html)
    if tree:
        thread = tree.get("thread", {})
        year_list = thread.get("year_list")
        if isinstance(year_list, list):
            for item in year_list:
                if isinstance(item, dict):
                    year_str = item.get("year")
                else:
                    year_str = item
                if year_str and re.match(r"^\d{4}$", str(year_str)):
                    years.add(int(year_str))
        elif isinstance(year_list, list) and year_list and isinstance(year_list[0], str):
            for year_str in year_list:
                if re.match(r"^\d{4}$", str(year_str)):
                    years.add(int(year_str))

    if years:
        return sorted(years)

    # Fallback: scrape year links from the rendered HTML
    year_pattern = re.compile(re.escape(base_slug) + r"-year/(\d{4})")
    for m in year_pattern.finditer(html):
        years.add(int(m.group(1)))
    return sorted(years)


def discover_subjects_from_category(client: MenggyClient, category_path: str) -> Dict[str, Dict]:
    """
    Parse a subject category page (e.g. /usnews-university-subject-rankings)
    and return a dict: {slug: {slug, name_zh, years, system, category}}.
    """
    result: Dict[str, Dict] = {}
    html = client.get_html(category_path)
    tree = parse_result_tree(html)
    if not tree:
        logger.warning("No result_tree found on category page %s", category_path)
        return result

    thread = tree.get("thread", {})
    system_prefix = category_path.strip("/").split("-")[0].lower()
    category_name = thread.get("name", category_path.strip("/").replace("-", " ").title())

    for tag in thread.get("tags", []):
        tag_name = tag.get("name", "其他")
        for subj in tag.get("threads", []):
            url = subj.get("url", "")
            if not url:
                continue
            # URL may be absolute, relative, or already a slug
            if url.startswith(BASE_URL):
                url = url[len(BASE_URL):]
            slug = url.lstrip("/")
            if not slug:
                continue
            years = []
            for item in subj.get("year_list", []):
                if isinstance(item, dict):
                    year_str = item.get("year")
                else:
                    year_str = item
                if year_str and re.match(r"^\d{4}$", str(year_str)):
                    years.append(int(year_str))
            result[slug] = {
                "slug": slug,
                "name_zh": subj.get("name"),
                "years": sorted(set(years)),
                "system": system_prefix,
                "category": tag_name,
            }
    return result


def discover_from_category_pages(client: MenggyClient) -> Dict[str, Dict]:
    """Discover all subjects/years from the authoritative category overview pages."""
    all_subjects: Dict[str, Dict] = {}
    for idx, path in enumerate(SUBJECT_CATEGORY_PAGES, 1):
        logger.info("[%d/%d] Discovering subjects from category page %s",
                    idx, len(SUBJECT_CATEGORY_PAGES), path)
        try:
            subjects = discover_subjects_from_category(client, path)
            logger.info("Category %s -> %d subjects", path, len(subjects))
            all_subjects.update(subjects)
        except Exception as exc:
            logger.error("Failed to discover from %s: %s", path, exc)
    return all_subjects


def discover(client: MenggyClient, use_cache: bool = True,
             max_workers: int = 4) -> List[Dict]:
    """
    Return a list of ranking list dicts to crawl:
    [{slug, year, name_zh, system, category}, ...]

    Uses the subject category pages as the authoritative source (they contain
    the full year_list for every subject), then merges with sitemap-discovered
    base ranking overview pages so no list is missed.
    """
    if use_cache and os.path.exists(DISCOVERY_CACHE):
        logger.info("Loading discovery cache from %s", DISCOVERY_CACHE)
        with open(DISCOVERY_CACHE, "r", encoding="utf-8") as f:
            return json.load(f)

    logger.info("Starting discovery from category pages...")
    subject_map = discover_from_category_pages(client)

    logger.info("Starting discovery from sitemap...")
    urls = fetch_sitemap_urls(client)
    slugs = extract_ranking_slugs(urls)
    logger.info(
        "Sitemap found base=%d subject=%d",
        len(slugs["base"]), len(slugs["subject"])
    )

    # Merge sitemap subject slugs (fallback / validation), normalising leading slashes
    for raw_slug in slugs["subject"]:
        slug = raw_slug.lstrip("/")
        if slug not in subject_map:
            subject_map[slug] = {
                "slug": slug,
                "name_zh": None,
                "years": [],
                "system": categorize_slug(slug)[0].lower(),
                "category": "unknown",
            }

    targets_map: Dict[Tuple[str, int], Dict] = {}

    # Expand category-discovered subjects with their full year list
    for info in subject_map.values():
        slug = info["slug"]
        if not info.get("years"):
            # No years from category page: try the overview page directly
            try:
                html = client.get_html(f"/{slug}")
                info["years"] = parse_years_from_overview(html, slug)
                logger.info("Overview %s -> %d years", slug, len(info["years"]))
            except Exception as exc:
                logger.error("Failed to discover years for %s: %s", slug, exc)
        for year in info.get("years", []):
            targets_map[(slug, year)] = {
                "slug": slug,
                "year": year,
                "name_zh": info.get("name_zh"),
                "system": info.get("system", "unknown"),
                "category": info.get("category", "unknown"),
            }

    # Add base ranking years from sitemap (avoids fetching 60+ overview pages)
    base_years = extract_base_years_from_sitemap(urls)
    logger.info("Sitemap provides years for %d base rankings", len(base_years))
    for slug, years in sorted(base_years.items()):
        if not years:
            continue
        for year in sorted(years):
            targets_map[(slug, year)] = {
                "slug": slug,
                "year": year,
                "name_zh": None,
                "system": categorize_slug(slug)[0].lower(),
                "category": "base",
            }

    # Optional: fetch years for any sitemap subject not covered by category pages
    sitemap_subject_slugs = {s.lstrip("/") for s in slugs["subject"]}
    missing_subject_slugs = sorted(sitemap_subject_slugs - set(subject_map.keys()))
    if missing_subject_slugs:
        logger.info("Fetching overview pages for %d sitemap-only subjects",
                    len(missing_subject_slugs))
        for idx, raw_slug in enumerate(missing_subject_slugs, 1):
            slug = raw_slug.lstrip("/")
            try:
                html = client.get_html(f"/{slug}")
                years = parse_years_from_overview(html, slug)
                if not years:
                    continue
                for year in years:
                    targets_map[(slug, year)] = {
                        "slug": slug,
                        "year": year,
                        "name_zh": None,
                        "system": categorize_slug(slug)[0].lower(),
                        "category": "unknown",
                    }
            except Exception as exc:
                logger.error("Failed to discover sitemap subject %s: %s", slug, exc)

    targets = list(targets_map.values())
    logger.info("Discovery complete: %d ranking lists to crawl", len(targets))

    if use_cache:
        with open(DISCOVERY_CACHE, "w", encoding="utf-8") as f:
            json.dump(targets, f, ensure_ascii=False, indent=2)

    return targets


def categorize_slug(slug: str) -> Tuple[str, str]:
    """Return (system_slug, human readable name) guess."""
    parts = slug.strip("/").split("-")
    system = parts[0].upper() if parts else "UNKNOWN"
    return system, slug.strip("/").replace("-", " ").title()
