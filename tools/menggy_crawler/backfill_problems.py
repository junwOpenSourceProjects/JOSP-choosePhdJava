"""Backfill the lists reported by audit_problems_subjects.json."""
import json
import logging
import sys

import pymysql

from client import MenggyClient
from config import DB_HOST, DB_PORT, DB_USER, DB_PASSWORD
from crawler import RankingCrawler
from discover import categorize_slug
from storage import Storage

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)],
)
logger = logging.getLogger(__name__)


def main():
    with open("audit_problems_subjects.json", encoding="utf-8") as f:
        problems = json.load(f)

    client = MenggyClient()
    storage = Storage()
    crawler = RankingCrawler(client, storage, max_workers=5)

    conn = pymysql.connect(
        host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASSWORD,
        database="menggy_rankings", charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )
    cur = conn.cursor()

    # Ensure every list row exists and clear stale entries
    for p in problems:
        slug = p["slug"]
        year = p["year"]
        cur.execute("SELECT id FROM ranking_lists WHERE slug=%s AND year=%s", (slug, year))
        row = cur.fetchone()
        if row:
            list_id = row["id"]
        else:
            system_code, _ = categorize_slug(slug)
            system_id = storage.insert_system(system_code.lower(), system_code.upper())
            list_id = storage.insert_list(system_id, slug, year)
            cur.execute("SELECT id FROM ranking_lists WHERE slug=%s AND year=%s", (slug, year))
            list_id = cur.fetchone()["id"]
        cur.execute("DELETE FROM ranking_entries WHERE list_id=%s", (list_id,))
    conn.commit()
    conn.close()

    # Re-crawl
    for idx, p in enumerate(problems, 1):
        slug = p["slug"]
        year = p["year"]
        try:
            cnt = crawler.crawl_list(slug, year)
            logger.info("[%d/%d] %s %s -> %d entries", idx, len(problems), slug, year, cnt)
        except Exception as exc:
            logger.error("[%d/%d] FAILED %s %s: %s", idx, len(problems), slug, year, exc)

    logger.info("Backfill complete")


if __name__ == "__main__":
    main()
