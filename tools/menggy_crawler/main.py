"""Entry point for Menggy rankings crawler."""
import argparse
import json
import logging
import sys

from client import MenggyClient
from config import validate
from crawler import RankingCrawler
from storage import Storage

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    handlers=[
        logging.FileHandler("menggy_crawler.log", encoding="utf-8"),
        logging.StreamHandler(sys.stdout),
    ],
)
logger = logging.getLogger(__name__)


def main():
    parser = argparse.ArgumentParser(description="Crawl daxue.menggy.com into MySQL")
    parser.add_argument("--init-db", action="store_true", help="Create database and tables")
    parser.add_argument("--rankings", action="store_true", help="Crawl all ranking lists")
    parser.add_argument("--details", action="store_true", help="Crawl university detail pages")
    parser.add_argument("--force-details", action="store_true",
                        help="Re-crawl all university detail pages even if already present")
    parser.add_argument("--all", action="store_true", help="Run full crawl (rankings + details)")
    parser.add_argument("--audit", action="store_true", help="Audit DB vs website totals")
    parser.add_argument("--backfill", action="store_true", help="Re-crawl missing/incomplete lists")
    parser.add_argument("--recrawl", action="store_true", help="Force re-crawl all lists (ignore completion cache)")
    parser.add_argument("--no-skip-completed", action="store_true",
                        help="Do not skip lists already marked successful")
    parser.add_argument("--no-login", action="store_true",
                        help="Skip member login (uses public entity API only)")
    parser.add_argument("--subjects-only", action="store_true",
                        help="Limit audit/backfill to subject rankings only")
    parser.add_argument("--limit", type=int, default=None, help="Limit number of ranking lists (testing)")
    parser.add_argument("--workers", type=int, default=5, help="Number of concurrent workers")
    parser.add_argument("--audit-output", type=str, default="audit_problems.json",
                        help="File to write audit problems to")
    args = parser.parse_args()

    validate()

    storage = Storage()
    if args.init_db or args.all:
        logger.info("Initializing database...")
        storage.init_database()

    if not any([args.rankings, args.details, args.all, args.audit, args.backfill, args.recrawl]):
        parser.print_help()
        return

    client = MenggyClient()
    if args.no_login:
        logger.info("Running without login (public entity API only)")
    else:
        logger.info("Logging in...")
        if not client.login():
            logger.error("Login failed")
            sys.exit(1)

    crawler = RankingCrawler(client, storage, max_workers=args.workers)

    if args.audit:
        problems = crawler.audit_rankings(subjects_only=args.subjects_only)
        with open(args.audit_output, "w", encoding="utf-8") as f:
            json.dump(problems, f, ensure_ascii=False, indent=2)
        logger.info("Wrote %d audit problems to %s", len(problems), args.audit_output)
        return

    if args.backfill:
        crawler.backfill_rankings(subjects_only=args.subjects_only)

    if args.rankings or args.all or args.recrawl:
        crawler.discover_and_crawl_rankings(
            limit=args.limit,
            skip_completed=(not args.recrawl and not args.no_skip_completed),
        )

    if args.details or args.all:
        crawler.crawl_university_details(force=args.force_details)

    logger.info("Done.")


if __name__ == "__main__":
    main()
