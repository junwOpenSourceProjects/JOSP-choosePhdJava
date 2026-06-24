"""Configuration for Menggy university rankings crawler."""
import os
from dotenv import load_dotenv

load_dotenv()

# Site credentials
MENGGY_EMAIL = os.getenv("MENGGY_EMAIL", "")
MENGGY_PASSWORD = os.getenv("MENGGY_PASSWORD", "")

# MySQL connection
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", "3306"))
DB_USER = os.getenv("DB_USER", "root")
DB_PASSWORD = os.getenv("DB_PASSWORD", "")
DB_NAME = os.getenv("DB_NAME", "menggy_rankings")

# Crawler behavior
REQUEST_DELAY = float(os.getenv("REQUEST_DELAY", "0.5"))  # seconds between requests
MAX_RETRIES = int(os.getenv("MAX_RETRIES", "3"))
RETRY_BACKOFF = float(os.getenv("RETRY_BACKOFF", "2.0"))
PAGE_SIZE = 10  # entities per page from /api/entity

# Endpoints
BASE_URL = "https://daxue.menggy.com"
LOGIN_API = "/api/login"
ENTITY_API_TEMPLATE = "/api/entity/{slug}"
DOWNLOAD_API_TEMPLATE = "/api/download/{slug}"
SEARCH_API = "/api/search"

# Subject category overview pages (used as authoritative discovery source)
SUBJECT_CATEGORY_PAGES = [
    "/qs-university-subject-rankings",
    "/arwu-university-subject-rankings",
    "/the-university-subject-rankings",
    "/usnews-university-subject-rankings",
    "/rur-university-subject-rankings",
]

# User agent
USER_AGENT = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
)


def validate():
    """Ensure required database configuration is present."""
    missing = []
    if not DB_HOST or DB_PORT <= 0:
        missing.append("DB_HOST/DB_PORT")
    if not DB_USER:
        missing.append("DB_USER")
    if missing:
        raise ValueError(f"Missing required config (env vars): {', '.join(missing)}")
