"""Data cleaning utilities."""
import logging
import re
from typing import Optional, Tuple

logger = logging.getLogger(__name__)


def parse_rank(rank_display: str, rank_raw: Optional[str] = None) -> Tuple[
    Optional[int], Optional[int], Optional[int], Optional[str]
]:
    """
    Parse rank strings into numeric components.
    Returns: (rank_int, range_start, range_end, normalized_display)
    """
    if not rank_display:
        return None, None, None, None

    text = str(rank_display).strip().replace("#", "").replace("=", "")

    # Range like "1001-1200"
    m = re.match(r"^(\d+)\s*-\s*(\d+)$", text)
    if m:
        start = int(m.group(1))
        end = int(m.group(2))
        return start, start, end, rank_display

    # Pure number
    m = re.match(r"^(\d+)$", text)
    if m:
        val = int(m.group(1))
        return val, None, None, rank_display

    # "=2" already handled by replace above
    m = re.match(r"^(\d+)\s*-\s*(\d+)$", text.replace("=", ""))
    if m:
        start = int(m.group(1))
        end = int(m.group(2))
        return start, start, end, rank_display

    # Fallback: try extract first integer
    nums = re.findall(r"\d+", text)
    if nums:
        val = int(nums[0])
        return val, None, None, rank_display

    return None, None, None, rank_display


def normalize_name(name: Optional[str]) -> Optional[str]:
    if not name:
        return None
    return name.strip()


def extract_country_region(tags: list) -> Tuple[Optional[str], Optional[str]]:
    """From entity tags, extract country and region names."""
    country = None
    region = None
    for tag in tags:
        url = tag.get("url", "")
        name = tag.get("name", "")
        if "-country/" in url:
            country = name
        elif "-region/" in url:
            region = name
    return country, region


def tag_type_by_url(url: str) -> Optional[str]:
    if "-country/" in url:
        return "country"
    if "-region/" in url:
        return "region"
    return "other"
