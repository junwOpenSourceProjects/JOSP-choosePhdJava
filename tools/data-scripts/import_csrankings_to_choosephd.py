#!/usr/bin/env python3
"""
Import CSRankings data from `csrankings` DB into `choosephd` project DB.

Creates ranking sources:
- CSRankings 综合 (overall)
- CSRankings AI
- CSRankings Systems
- CSRankings Theory
- CSRankings Interdisciplinary

Aggregates faculty_publication_count by institution × top-category × year,
maps institutions to choosephd universities by name, computes rank within
each source/year, and inserts ranking_entry records with score.
"""

import argparse
import logging
import re
import sys
from difflib import SequenceMatcher
from typing import Dict, List, Optional, Tuple

import pymysql
from pymysql.cursors import DictCursor

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger(__name__)

SOURCE_DB = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "",
    "database": "csrankings",
    "charset": "utf8mb4",
}

TARGET_DB = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "",
    "database": "choosephd",
    "charset": "utf8mb4",
}

# top_category -> (source_slug, subject_slug, subject_name_zh, subject_name_en)
CSR_AREAS = {
    "ai": ("csrankings-ai", "csrankings-ai", "CSRankings AI", "CSRankings AI"),
    "systems": ("csrankings-systems", "csrankings-systems", "CSRankings Systems", "CSRankings Systems"),
    "theory": ("csrankings-theory", "csrankings-theory", "CSRankings Theory", "CSRankings Theory"),
    "interdisciplinary": ("csrankings-interdisciplinary", "csrankings-interdisciplinary", "CSRankings Interdisciplinary", "CSRankings Interdisciplinary"),
}

OVERALL_SOURCE = ("csrankings-overall", "CSRankings 综合", "CSRankings Overall")


def connect(cfg: dict):
    return pymysql.connect(cursorclass=DictCursor, **cfg)


def normalize_name(name: str) -> str:
    """Normalize institution/university name for matching."""
    text = name.lower()
    text = re.sub(r"[\-_\.,]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    # Remove common suffixes/words that differ between sources
    removable = [
        "university", "college", "institute of technology", "institute",
        "the ", "of ", " at ", " and ", " & ", " massachusetts", " ma ",
    ]
    for word in removable:
        text = text.replace(word, " ")
    text = re.sub(r"\s+", " ", text).strip()
    return text


def token_set(name: str) -> set:
    return set(normalize_name(name).split())


def match_score(a: str, b: str) -> float:
    """Hybrid name similarity: token overlap + sequence ratio + substring bonus."""
    norm_a = normalize_name(a)
    norm_b = normalize_name(b)

    # Exact or substring match bonus
    if norm_a == norm_b:
        return 1.0
    if len(norm_a) >= 3 and norm_a in norm_b:
        return 0.95
    if len(norm_b) >= 3 and norm_b in norm_a:
        return 0.95

    tokens_a = token_set(a)
    tokens_b = token_set(b)
    if not tokens_a or not tokens_b:
        return 0.0
    overlap = len(tokens_a & tokens_b)
    union = len(tokens_a | tokens_b)
    token_ratio = overlap / union if union else 0.0
    if token_ratio >= 0.85:
        return token_ratio
    seq_ratio = SequenceMatcher(None, norm_a, norm_b).ratio()
    return max(token_ratio, seq_ratio)


def build_university_index(target) -> List[dict]:
    with target.cursor() as cur:
        cur.execute(
            "SELECT url_id, name_zh, name_en, country FROM university WHERE deleted = 0"
        )
        rows = cur.fetchall()
    index = []
    for r in rows:
        candidates = []
        if r["name_en"]:
            candidates.append(r["name_en"])
        if r["name_zh"]:
            candidates.append(r["name_zh"])
        index.append({
            "url_id": r["url_id"],
            "names": candidates,
            "country": r["country"],
        })
    return index


# Common abbreviation / short-name overrides for institutions that are hard to fuzzy-match.
INSTITUTION_ALIASES = {
    "AUEB": ["Athens University Of Economics And Business"],
    "BITS Pilani": ["Birla Institute Of Technology And Science Pilani"],
    "BNBU": ["Beijing Normal University At Zhuhai"],
    "BUPT": ["Beijing University Of Posts And Telecommunications"],
    "BUET": ["Bangladesh University Of Engineering And Technology"],
    "CMI": ["Chennai Mathematical Institute"],
    "CRIStAL": ["Centre De Recherche En Informatique, Signal Et Automatique De Lille"],
    "CUNY": ["City University Of New York"],
    "CWI": ["Centrum Wiskunde Informatica"],
    "CISPA Helmholtz Center": ["Cispa"],
    "CNRS": ["French National Centre For Scientific Research"],
    "CUHK (SZ)": ["Chinese University Of Hong Kong, Shenzhen"],
    "DAIICT": ["Dhirubhai Ambani Institute Of Information And Communication Technology"],
    "DGIST": ["Daegu Gyeongbuk Institute Of Science And Technology"],
    "DTU": ["Technical University Of Denmark"],
    "EPFL": ["Swiss Federal Institute Of Technology Lausanne"],
    "GIST": ["Gwangju Institute Of Science And Technology"],
    "GSSI": ["Gran Sasso Science Institute"],
    "Hasso Plattner Institute": ["Hasso Plattner Institute For Digital Engineering"],
    "HKUST": ["Hong Kong University Of Science And Technology"],
    "HSE University": ["Higher School Of Economics"],
    "HUST": ["Huazhong University Of Science And Technology"],
    "IACS": ["Indian Association For The Cultivation Of Science"],
    "IGDTUW": ["Indira Gandhi Delhi Technical University For Women"],
    "IIT (ISM) Dhanbad": ["Indian Institute Of Technology (Indian School Of Mines) Dhanbad"],
    "IIT Bhilai": ["Indian Institute Of Technology Bhilai"],
    "IIT Goa": ["Indian Institute Of Technology Goa"],
    "IIT Palakkad": ["Indian Institute Of Technology Palakkad"],
    "IIT Tirupati": ["Indian Institute Of Technology Tirupati"],
    "IMDEA Networks Institute": ["Imdea Networks Institute"],
    "IMDEA Software Institute": ["Imdea Software Institute"],
    "IMSc": ["Institute Of Mathematical Sciences"],
    "INRIA": ["Institut National De Recherche En Informatique Et Automatique"],
    "INSAIT": ["Institute For Computer Science, Artificial Intelligence And Technology"],
    "IPI PAN": ["Institute Of Computer Science, Polish Academy Of Sciences"],
    "IPM": ["Institute For Research In Fundamental Sciences"],
    "IST Austria": ["Institute Of Science And Technology Austria"],
    "IUPUI": ["Indiana University Purdue University Indianapolis"],
    "Jacobs University Bremen": ["Jacobs University"],
    "JKU Linz": ["Johannes Kepler University Linz"],
    "JUST": ["Jordan University Of Science And Technology"],
    "KAIST": ["Korea Advanced Institute Of Science And Technology"],
    "KAUST": ["King Abdullah University Of Science And Technology"],
    "Kempelen Institute - KInIT": ["Kempelen Institute Of Intelligent Technologies"],
    "LIU Post": ["Long Island University Post"],
    "NTNU": ["Norwegian University Of Science And Technology"],
    "NWPU": ["Northwestern Polytechnical University"],
    "POSTECH": ["Pohang University Of Science And Technology"],
    "TU Berlin": ["Technical University Of Berlin"],
    "TU Darmstadt": ["Technical University Of Darmstadt"],
    "TU Dresden": ["Technical University Of Dresden"],
    "TU Munich": ["Technical University Of Munich"],
    "TU Wien": ["Vienna University Of Technology"],
    "UMass": ["University Of Massachusetts"],
    "USC": ["University Of Southern California"],
    "USTC": ["University Of Science And Technology Of China"],
    "UVA": ["University Of Virginia"],
}


def build_university_index(target) -> Tuple[List[dict], Dict[str, List[str]]]:
    with target.cursor() as cur:
        cur.execute(
            "SELECT url_id, name_zh, name_en, country FROM university WHERE deleted = 0"
        )
        rows = cur.fetchall()
    index = []
    token_to_urls: Dict[str, List[str]] = {}
    for r in rows:
        candidates = []
        if r["name_en"]:
            candidates.append(r["name_en"])
        if r["name_zh"]:
            candidates.append(r["name_zh"])
        entry = {
            "url_id": r["url_id"],
            "names": candidates,
            "country": r["country"],
        }
        index.append(entry)
        for cand in candidates:
            tokens = token_set(cand)
            for token in tokens:
                token_to_urls.setdefault(token, []).append(r["url_id"])
            # Add first-letter acronym token to help match abbreviations like MIT, BUPT
            words = [w for w in normalize_name(cand).split() if len(w) >= 2]
            if len(words) >= 2:
                acronym = "".join(w[0] for w in words)
                if len(acronym) >= 2:
                    token_to_urls.setdefault(acronym, []).append(r["url_id"])
    return index, token_to_urls


def match_institution(name: str, index: List[dict], token_to_urls: Dict[str, List[str]],
                      threshold: float = 0.65) -> Optional[str]:
    # Try explicit aliases first
    if name in INSTITUTION_ALIASES:
        for alias in INSTITUTION_ALIASES[name]:
            alias_norm = normalize_name(alias)
            for u in index:
                for cand in u["names"]:
                    if alias_norm in normalize_name(cand):
                        return u["url_id"]

    tokens = token_set(name)
    if not tokens:
        return None

    # Candidate set: universities sharing at least one token
    candidate_ids: set = set()
    for token in tokens:
        candidate_ids.update(token_to_urls.get(token, []))

    url_to_entry = {u["url_id"]: u for u in index}
    best_url_id = None
    best_score = 0.0

    for url_id in candidate_ids:
        u = url_to_entry[url_id]
        for candidate in u["names"]:
            score = match_score(name, candidate)
            if score > best_score:
                best_score = score
                best_url_id = u["url_id"]

    if best_score >= threshold:
        return best_url_id
    return None


def build_institution_mapping(source, index: List[dict], token_to_urls: Dict[str, List[str]],
                              threshold: float = 0.65) -> Tuple[Dict[int, str], List[str]]:
    """Match each distinct csrankings institution to a choosephd university once."""
    with source.cursor() as cur:
        cur.execute("SELECT id, name FROM institution ORDER BY id")
        institutions = cur.fetchall()

    mapping: Dict[int, str] = {}
    unmatched: List[str] = []
    total = len(institutions)
    for i, inst in enumerate(institutions, 1):
        url_id = match_institution(inst["name"], index, token_to_urls, threshold)
        if url_id:
            mapping[inst["id"]] = url_id
        else:
            unmatched.append(inst["name"])
        if i % 100 == 0 or i == total:
            logger.info("Matched institutions: %s/%s", i, total)
    return mapping, unmatched


def ensure_sources(target) -> Dict[str, int]:
    """Create CSRankings ranking sources and return slug -> id mapping."""
    sources = {}
    with target.cursor() as cur:
        # Overall
        cur.execute(
            "INSERT IGNORE INTO ranking_source (slug, name_zh, name_en, kind, owner_org, active) "
            "VALUES (%s, %s, %s, %s, %s, 1)",
            (OVERALL_SOURCE[0], OVERALL_SOURCE[1], OVERALL_SOURCE[2], 1, "CSR"),
        )
        # Areas (subject rankings)
        for top_cat, (slug, sub_slug, name_zh, name_en) in CSR_AREAS.items():
            cur.execute(
                "INSERT IGNORE INTO ranking_source (slug, name_zh, name_en, kind, owner_org, active) "
                "VALUES (%s, %s, %s, %s, %s, 1)",
                (slug, name_zh, name_en, 3, "CSR"),
            )
        target.commit()

        cur.execute("SELECT id, slug FROM ranking_source WHERE owner_org = 'CSR'")
        for row in cur.fetchall():
            sources[row["slug"]] = row["id"]
    return sources


def ensure_subjects(target) -> Dict[str, int]:
    """Create CSRankings subject entries and return top_category -> id mapping."""
    subjects = {}
    with target.cursor() as cur:
        for top_cat, (slug, sub_slug, name_zh, name_en) in CSR_AREAS.items():
            cur.execute(
                "INSERT IGNORE INTO subject (slug, name_zh, name_en, owner_org, active) "
                "VALUES (%s, %s, %s, %s, 1)",
                (sub_slug, name_zh, name_en, "CSR"),
            )
        target.commit()

        cur.execute("SELECT id, slug FROM subject WHERE owner_org = 'CSR'")
        for row in cur.fetchall():
            subjects[row["slug"]] = row["id"]
    return subjects


def fetch_csrankings_aggregates(source) -> List[dict]:
    """Aggregate adjusted_count by institution × top_category × year."""
    sql = """
        SELECT
            i.id AS institution_id,
            i.name AS institution_name,
            i.country_alpha2,
            ra.top_category,
            fpc.year,
            SUM(fpc.adjusted_count) AS total_score
        FROM institution i
        JOIN faculty f ON f.institution_id = i.id
        JOIN faculty_publication_count fpc ON fpc.faculty_id = f.id
        JOIN research_area ra ON ra.code = fpc.area_code
        WHERE ra.top_category IS NOT NULL
        GROUP BY i.id, i.name, i.country_alpha2, ra.top_category, fpc.year
        HAVING total_score > 0
        ORDER BY i.id, ra.top_category, fpc.year
    """
    with source.cursor() as cur:
        cur.execute(sql)
        return cur.fetchall()


def compute_ranks(entries: List[dict]) -> List[dict]:
    """Add rank_value and rank_display based on score descending."""
    # Group by (source_slug, year)
    groups: Dict[Tuple[str, int], List[dict]] = {}
    for e in entries:
        key = (e["source_slug"], e["year"])
        groups.setdefault(key, []).append(e)

    result = []
    for key, group in groups.items():
        group.sort(key=lambda x: x["score"], reverse=True)
        for rank, e in enumerate(group, start=1):
            e["rank_value"] = rank
            e["rank_display"] = str(rank)
            result.append(e)
    return result


def delete_existing_csr_entries(target):
    with target.cursor() as cur:
        cur.execute(
            "DELETE re FROM ranking_entry re "
            "JOIN ranking_source rs ON re.source_id = rs.id "
            "WHERE rs.owner_org = 'CSR'"
        )
        target.commit()
        logger.info("Deleted %s existing CSRankings ranking entries", cur.rowcount)


def insert_entries(target, entries: List[dict], source_ids: Dict[str, int], subject_ids: Dict[str, int]):
    if not entries:
        logger.info("No entries to insert")
        return

    overall_id = source_ids[OVERALL_SOURCE[0]]

    # Build mapping slug -> source_id
    def source_id_for(slug: str) -> int:
        return source_ids[slug]

    rows = []
    for e in entries:
        source_slug = e["source_slug"]
        subject_id = None
        if source_slug != OVERALL_SOURCE[0]:
            # area source -> link to subject
            _, sub_slug, _, _ = CSR_AREAS[e["top_category"]]
            subject_id = subject_ids.get(sub_slug)
        rows.append((
            e["url_id"],
            source_id_for(source_slug),
            subject_id,
            e["year"],
            e["rank_display"],
            e["rank_value"],
            e["score"],
        ))

    sql = """
        INSERT INTO ranking_entry
        (university_id, source_id, subject_id, year, rank_display, rank_value, score)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
    """
    with target.cursor() as cur:
        batch_size = 1000
        for i in range(0, len(rows), batch_size):
            batch = rows[i:i + batch_size]
            cur.executemany(sql, batch)
            target.commit()
            logger.info("Inserted %s/%s entries", min(i + batch_size, len(rows)), len(rows))


def main():
    parser = argparse.ArgumentParser(description="Import CSRankings into choosephd")
    parser.add_argument("--dry-run", action="store_true", help="Do not write to choosephd")
    parser.add_argument("--threshold", type=float, default=0.65, help="Name matching threshold")
    parser.add_argument("--skip-unmatched", action="store_true", help="Skip printing unmatched institutions")
    args = parser.parse_args()

    source = connect(SOURCE_DB)
    target = connect(TARGET_DB)
    try:
        logger.info("Building university index...")
        uni_index, token_to_urls = build_university_index(target)
        logger.info("University index size: %s", len(uni_index))

        logger.info("Ensuring ranking sources and subjects...")
        source_ids = ensure_sources(target)
        subject_ids = ensure_subjects(target)
        logger.info("Sources: %s", source_ids)
        logger.info("Subjects: %s", subject_ids)

        logger.info("Building institution mapping...")
        inst_mapping, unmatched = build_institution_mapping(source, uni_index, token_to_urls, args.threshold)
        logger.info("Mapped %s institutions, %s unmatched", len(inst_mapping), len(unmatched))

        logger.info("Fetching CSRankings aggregates...")
        aggregates = fetch_csrankings_aggregates(source)
        logger.info("Aggregate rows: %s", len(aggregates))

        # Map institutions to universities and aggregate by (url_id, top_category, year)
        area_score_map: Dict[Tuple[str, str, int], dict] = {}
        for row in aggregates:
            url_id = inst_mapping.get(row["institution_id"])
            if not url_id:
                continue
            top_cat = row["top_category"]
            key = (url_id, top_cat, row["year"])
            area_score_map.setdefault(key, {
                "url_id": url_id,
                "source_slug": CSR_AREAS[top_cat][0],
                "year": row["year"],
                "score": 0.0,
                "top_category": top_cat,
            })
            area_score_map[key]["score"] += float(row["total_score"])

        area_entries: Dict[str, List[dict]] = {key: [] for key in CSR_AREAS.keys()}
        for e in area_score_map.values():
            area_entries[e["top_category"]].append(e)

        # Build overall entries by summing area scores per university/year
        overall_map: Dict[Tuple[str, int], dict] = {}
        for entries in area_entries.values():
            for e in entries:
                key = (e["url_id"], e["year"])
                if key not in overall_map:
                    overall_map[key] = {
                        "url_id": e["url_id"],
                        "source_slug": OVERALL_SOURCE[0],
                        "year": e["year"],
                        "score": 0.0,
                        "top_category": None,
                    }
                overall_map[key]["score"] += e["score"]

        all_entries = list(overall_map.values())
        for entries in area_entries.values():
            all_entries.extend(entries)

        logger.info("Total entries before ranking: %s", len(all_entries))
        if not args.skip_unmatched:
            logger.info("Unmatched institutions: %s", len(unmatched))
            for name in sorted(unmatched)[:30]:
                logger.info("  - %s", name)
            if len(unmatched) > 30:
                logger.info("  ... and %s more", len(unmatched) - 30)

        ranked_entries = compute_ranks(all_entries)
        logger.info("Total entries after ranking: %s", len(ranked_entries))

        if args.dry_run:
            logger.info("Dry run, skipping database writes")
            return

        delete_existing_csr_entries(target)
        insert_entries(target, ranked_entries, source_ids, subject_ids)
        logger.info("CSRankings import completed")

    finally:
        source.close()
        target.close()


if __name__ == "__main__":
    main()
