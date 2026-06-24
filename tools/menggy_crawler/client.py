"""HTTP client with authentication, shared rate limiting, retries."""
import json
import logging
import re
import threading
import time
from typing import Optional

import requests

from config import (
    BASE_URL,
    LOGIN_API,
    MAX_RETRIES,
    REQUEST_DELAY,
    RETRY_BACKOFF,
    USER_AGENT,
    MENGGY_EMAIL,
    MENGGY_PASSWORD,
)

logger = logging.getLogger(__name__)


class RateLimiter:
    """Thread-safe rate limiter ensuring minimum delay between requests."""

    def __init__(self, delay: float):
        self.delay = delay
        self._last_request_time = 0.0
        self._lock = threading.Lock()

    def wait(self):
        """Block until it is OK to make the next request."""
        with self._lock:
            now = time.time()
            elapsed = now - self._last_request_time
            if elapsed < self.delay:
                sleep_time = self.delay - elapsed
                time.sleep(sleep_time)
                self._last_request_time = time.time()
            else:
                self._last_request_time = now


class MenggyClient:
    """Session-based client for daxue.menggy.com (thread-safe for requests)."""

    def __init__(self, rate_limiter: Optional[RateLimiter] = None):
        self.session = requests.Session()
        self.session.headers.update(
            {
                "User-Agent": USER_AGENT,
                "Accept": "application/json, text/plain, */*",
                "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
                "X-Requested-With": "XMLHttpRequest",
            }
        )
        self.csrf_token: Optional[str] = None
        self.logged_in = False
        self.rate_limiter = rate_limiter or RateLimiter(REQUEST_DELAY)

    def _get_csrf(self, html: str) -> Optional[str]:
        match = re.search(r'const CSRF_TOKEN = "([^"]+)"', html)
        return match.group(1) if match else None

    def login(self) -> bool:
        """Log in and return True on success."""
        resp = self.session.get(BASE_URL, timeout=30)
        resp.raise_for_status()
        self.csrf_token = self._get_csrf(resp.text)
        if not self.csrf_token:
            raise RuntimeError("CSRF token not found on homepage")

        payload = {
            "data": json.dumps(
                {
                    "email": MENGGY_EMAIL,
                    "password": MENGGY_PASSWORD,
                    "action": "login",
                    "source": "menggy",
                    "name": "",
                }
            )
        }
        headers = {
            "X-CSRF-TOKEN": self.csrf_token,
            "Content-Type": "application/x-www-form-urlencoded",
            "Referer": BASE_URL + "/",
        }
        for attempt in range(MAX_RETRIES):
            try:
                resp = self.session.post(
                    BASE_URL + LOGIN_API, data=payload, headers=headers, timeout=30
                )
                resp.raise_for_status()
                data = resp.json()
                if data.get("code") == 200:
                    self.logged_in = True
                    logger.info("Login successful for %s", MENGGY_EMAIL)
                    return True
                else:
                    raise RuntimeError(f"Login failed: {data.get('msg')}")
            except Exception as exc:
                logger.warning("Login attempt %d failed: %s", attempt + 1, exc)
                if attempt == MAX_RETRIES - 1:
                    raise
                time.sleep(RETRY_BACKOFF ** attempt)
        return False

    def request(self, method: str, url: str, **kwargs) -> requests.Response:
        """Make a request with retries and global rate limiting."""
        full_url = url if url.startswith("http") else BASE_URL + url
        last_exc: Optional[Exception] = None
        for attempt in range(MAX_RETRIES):
            try:
                self.rate_limiter.wait()
                resp = self.session.request(method, full_url, timeout=30, **kwargs)
                resp.raise_for_status()
                return resp
            except requests.RequestException as exc:
                last_exc = exc
                logger.warning(
                    "Request %s %s failed (attempt %d): %s",
                    method,
                    full_url,
                    attempt + 1,
                    exc,
                )
                if attempt < MAX_RETRIES - 1:
                    time.sleep(RETRY_BACKOFF ** attempt)
        raise last_exc or RuntimeError(f"Failed to fetch {full_url}")

    def get_json(self, url: str, **kwargs):
        resp = self.request("GET", url, **kwargs)
        return resp.json()

    def get_html(self, url: str, **kwargs) -> str:
        resp = self.request("GET", url, **kwargs)
        return resp.text
