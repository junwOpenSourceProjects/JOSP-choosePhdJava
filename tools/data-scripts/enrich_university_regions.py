#!/usr/bin/env python3
"""
Enrich choosephd.university country/region from all available menggy tags and name heuristics.
Goal: zero unknown country / zero unknown region.
"""
import logging
import re
import sys
from typing import Dict, List, Optional, Set, Tuple

import pymysql
from pymysql.cursors import DictCursor

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")
logger = logging.getLogger(__name__)

SOURCE = {"host": "localhost", "port": 3306, "user": "root", "password": "", "database": "menggy_rankings", "charset": "utf8mb4"}
TARGET = {"host": "localhost", "port": 3306, "user": "root", "password": "", "database": "choosephd", "charset": "utf8mb4"}

CONTINENTS = {"亚洲", "欧洲", "北美洲", "非洲", "南美洲", "大洋洲", "南极洲"}

CHINESE_PROVINCES = {
    "山东", "山西", "江苏", "河南", "新疆", "河北", "湖北", "湖南", "广东", "广西", "海南", "四川", "贵州", "云南", "西藏",
    "陕西", "甘肃", "青海", "辽宁", "吉林", "黑龙江", "内蒙古", "宁夏", "北京", "天津", "上海", "重庆", "香港", "澳门", "台湾",
}

UNIVERSITY_TYPES = {
    "综合类", "师范类", "理工类", "政法类", "艺术类", "医药类", "财经类", "体育类", "农林类", "民族类", "语言类", "军事类",
    "私立", "公立", "国立", "211", "985", "双一流", "双非", "C9", "常春藤", "藤校",
}

IGNORED_TAGS = CONTINENTS | CHINESE_PROVINCES | UNIVERSITY_TYPES | {"其他"}

# Country -> continent mapping (inverse of UniversityService.CONTINENT_COUNTRIES)
COUNTRY_TO_CONTINENT = {
    # 亚洲
    "中国": "亚洲", "中国台湾": "亚洲", "中国香港": "亚洲", "中国澳门": "亚洲",
    "蒙古": "亚洲", "朝鲜": "亚洲", "韩国": "亚洲", "日本": "亚洲",
    "越南": "亚洲", "老挝": "亚洲", "柬埔寨": "亚洲", "泰国": "亚洲", "缅甸": "亚洲",
    "马来西亚": "亚洲", "新加坡": "亚洲", "印度尼西亚": "亚洲", "文莱": "亚洲", "菲律宾": "亚洲", "东帝汶": "亚洲",
    "印度": "亚洲", "巴基斯坦": "亚洲", "孟加拉国": "亚洲", "尼泊尔": "亚洲", "不丹": "亚洲",
    "斯里兰卡": "亚洲", "马尔代夫": "亚洲", "哈萨克斯坦": "亚洲", "吉尔吉斯斯坦": "亚洲", "塔吉克斯坦": "亚洲",
    "乌兹别克斯坦": "亚洲", "土库曼斯坦": "亚洲", "阿富汗": "亚洲", "伊拉克": "亚洲", "伊朗": "亚洲",
    "叙利亚": "亚洲", "黎巴嫩": "亚洲", "以色列": "亚洲", "巴勒斯坦": "亚洲", "约旦": "亚洲",
    "沙特阿拉伯": "亚洲", "也门": "亚洲", "阿曼": "亚洲", "阿联酋": "亚洲", "卡塔尔": "亚洲", "巴林": "亚洲", "科威特": "亚洲",
    "土耳其": "亚洲", "阿塞拜疆": "亚洲", "格鲁吉亚": "亚洲", "亚美尼亚": "亚洲", "塞浦路斯": "亚洲",
    # 欧洲
    "英国": "欧洲", "爱尔兰": "欧洲", "法国": "欧洲", "荷兰": "欧洲", "比利时": "欧洲", "卢森堡": "欧洲",
    "德国": "欧洲", "奥地利": "欧洲", "瑞士": "欧洲", "波兰": "欧洲", "捷克": "欧洲", "斯洛伐克": "欧洲",
    "匈牙利": "欧洲", "罗马尼亚": "欧洲", "保加利亚": "欧洲", "塞尔维亚": "欧洲", "克罗地亚": "欧洲",
    "斯洛文尼亚": "欧洲", "黑山": "欧洲", "北马其顿": "欧洲", "波黑": "欧洲", "阿尔巴尼亚": "欧洲", "希腊": "欧洲",
    "意大利": "欧洲", "西班牙": "欧洲", "葡萄牙": "欧洲", "马耳他": "欧洲", "安道尔": "欧洲", "圣马力诺": "欧洲",
    "梵蒂冈": "欧洲", "摩纳哥": "欧洲", "列支敦士登": "欧洲", "丹麦": "欧洲", "挪威": "欧洲", "瑞典": "欧洲",
    "芬兰": "欧洲", "冰岛": "欧洲", "爱沙尼亚": "欧洲", "拉脱维亚": "欧洲", "立陶宛": "欧洲",
    "白俄罗斯": "欧洲", "俄罗斯": "欧洲", "乌克兰": "欧洲", "摩尔多瓦": "欧洲",
    # 北美洲
    "美国": "北美洲", "加拿大": "北美洲", "墨西哥": "北美洲", "危地马拉": "北美洲", "伯利兹": "北美洲",
    "萨尔瓦多": "北美洲", "洪都拉斯": "北美洲", "尼加拉瓜": "北美洲", "哥斯达黎加": "北美洲", "巴拿马": "北美洲",
    "古巴": "北美洲", "牙买加": "北美洲", "海地": "北美洲", "多米尼加": "北美洲", "巴哈马": "北美洲",
    "巴巴多斯": "北美洲", "特立尼达和多巴哥": "北美洲", "格林纳达": "北美洲", "圣卢西亚": "北美洲",
    "圣文森特和格林纳丁斯": "北美洲", "安提瓜和巴布达": "北美洲", "圣基茨和尼维斯": "北美洲", "多米尼克": "北美洲",
    # 非洲
    "埃及": "非洲", "利比亚": "非洲", "突尼斯": "非洲", "阿尔及利亚": "非洲", "摩洛哥": "非洲", "苏丹": "非洲",
    "南苏丹": "非洲", "乍得": "非洲", "尼日尔": "非洲", "马里": "非洲", "布基纳法索": "非洲", "毛里塔尼亚": "非洲",
    "塞内加尔": "非洲", "冈比亚": "非洲", "几内亚": "非洲", "几内亚比绍": "非洲", "塞拉利昂": "非洲", "利比里亚": "非洲",
    "科特迪瓦": "非洲", "加纳": "非洲", "多哥": "非洲", "贝宁": "非洲", "尼日利亚": "非洲", "喀麦隆": "非洲",
    "中非共和国": "非洲", "赤道几内亚": "非洲", "加蓬": "非洲", "刚果（布）": "非洲", "刚果（金）": "非洲",
    "安哥拉": "非洲", "赞比亚": "非洲", "马拉维": "非洲", "莫桑比克": "非洲", "纳米比亚": "非洲",
    "博茨瓦纳": "非洲", "津巴布韦": "非洲", "南非": "非洲", "斯威士兰": "非洲", "莱索托": "非洲",
    "马达加斯加": "非洲", "毛里求斯": "非洲", "科摩罗": "非洲", "塞舌尔": "非洲", "佛得角": "非洲",
    "圣多美和普林西比": "非洲", "埃塞俄比亚": "非洲", "厄立特里亚": "非洲", "吉布提": "非洲", "索马里": "非洲",
    "肯尼亚": "非洲", "乌干达": "非洲", "坦桑尼亚": "非洲", "卢旺达": "非洲", "布隆迪": "非洲", "刚果": "非洲",
    # 南美洲
    "巴西": "南美洲", "阿根廷": "南美洲", "智利": "南美洲", "乌拉圭": "南美洲", "巴拉圭": "南美洲",
    "玻利维亚": "南美洲", "秘鲁": "南美洲", "哥伦比亚": "南美洲", "委内瑞拉": "南美洲", "厄瓜多尔": "南美洲",
    "圭亚那": "南美洲", "苏里南": "南美洲", "法属圭亚那": "南美洲",
    # 大洋洲
    "澳大利亚": "大洋洲", "新西兰": "大洋洲", "巴布亚新几内亚": "大洋洲", "斐济": "大洋洲", "所罗门群岛": "大洋洲",
    "萨摩亚": "大洋洲", "汤加": "大洋洲", "瓦努阿图": "大洋洲", "基里巴斯": "大洋洲", "瑙鲁": "大洋洲",
    "图瓦卢": "大洋洲", "帕劳": "大洋洲", "密克罗尼西亚": "大洋洲", "马绍尔群岛": "大洋洲",
}

# Extra country name normalizations / aliases
COUNTRY_ALIASES = {
    "usa": "美国", "united states": "美国", "america": "美国",
    "uk": "英国", "united kingdom": "英国", "britain": "英国", "england": "英国", "great britain": "英国",
    "russia": "俄罗斯", "soviet union": "俄罗斯",
    "korea": "韩国", "south korea": "韩国", "republic of korea": "韩国",
    "north korea": "朝鲜", "dprk": "朝鲜",
    "japan": "日本",
    "india": "印度",
    "germany": "德国",
    "france": "法国",
    "italy": "意大利",
    "spain": "西班牙",
    "canada": "加拿大",
    "australia": "澳大利亚",
    "brazil": "巴西",
    "netherlands": "荷兰",
    "switzerland": "瑞士",
    "sweden": "瑞典",
    "belgium": "比利时",
    "austria": "奥地利",
    "poland": "波兰",
    "turkey": "土耳其",
    "israel": "以色列",
    "singapore": "新加坡",
    "malaysia": "马来西亚",
    "thailand": "泰国",
    "indonesia": "印度尼西亚",
    "philippines": "菲律宾",
    "vietnam": "越南",
    "mexico": "墨西哥",
    "argentina": "阿根廷",
    "chile": "智利",
    "colombia": "哥伦比亚",
    "peru": "秘鲁",
    "venezuela": "委内瑞拉",
    "egypt": "埃及",
    "south africa": "南非",
    "nigeria": "尼日利亚",
    "kenya": "肯尼亚",
    "ethiopia": "埃塞俄比亚",
    "morocco": "摩洛哥",
    "ghana": "加纳",
    "algeria": "阿尔及利亚",
    "iran": "伊朗",
    "iraq": "伊拉克",
    "pakistan": "巴基斯坦",
    "bangladesh": "孟加拉国",
    "nepal": "尼泊尔",
    "sri lanka": "斯里兰卡",
    "myanmar": "缅甸", "burma": "缅甸",
    "cambodia": "柬埔寨",
    "laos": "老挝",
    "mongolia": "蒙古",
    "kazakhstan": "哈萨克斯坦",
    "uzbekistan": "乌兹别克斯坦",
    "ukraine": "乌克兰",
    "belarus": "白俄罗斯",
    "romania": "罗马尼亚",
    "czech republic": "捷克", "czechia": "捷克",
    "hungary": "匈牙利",
    "portugal": "葡萄牙",
    "greece": "希腊",
    "denmark": "丹麦",
    "finland": "芬兰",
    "norway": "挪威",
    "ireland": "爱尔兰",
    "new zealand": "新西兰",
    "cuba": "古巴",
    "dominican republic": "多米尼加",
    "guatemala": "危地马拉",
    "costa rica": "哥斯达黎加",
    "panama": "巴拿马",
    "honduras": "洪都拉斯",
    "elsalvador": "萨尔瓦多", "el salvador": "萨尔瓦多",
    "nicaragua": "尼加拉瓜",
    "puerto rico": "波多黎各",
    "jamaica": "牙买加",
    "trinidad and tobago": "特立尼达和多巴哥",
    "barbados": "巴巴多斯",
    "bahamas": "巴哈马",
    "bolivia": "玻利维亚",
    "ecuador": "厄瓜多尔",
    "paraguay": "巴拉圭",
    "uruguay": "乌拉圭",
    "guyana": "圭亚那",
    "suriname": "苏里南",
    "tunisia": "突尼斯",
    "libya": "利比亚",
    "sudan": "苏丹",
    "uganda": "乌干达",
    "tanzania": "坦桑尼亚",
    "zimbabwe": "津巴布韦",
    "zambia": "赞比亚",
    "botswana": "博茨瓦纳",
    "cameroon": "喀麦隆",
    "ivory coast": "科特迪瓦", "côte d'ivoire": "科特迪瓦",
    "senegal": "塞内加尔",
    "mali": "马里",
    "burkina faso": "布基纳法索",
    "niger": "尼日尔",
    "chad": "乍得",
    "guinea": "几内亚",
    "rwanda": "卢旺达",
    "burundi": "布隆迪",
    "togo": "多哥",
    "benin": "贝宁",
    "madagascar": "马达加斯加",
    "mauritius": "毛里求斯",
    "seychelles": "塞舌尔",
    "fiji": "斐济",
    "papua new guinea": "巴布亚新几内亚",
    "samoa": "萨摩亚",
    "tonga": "汤加",
    "vanuatu": "瓦努阿图",
    "solomon islands": "所罗门群岛",
    "croatia": "克罗地亚",
    "serbia": "塞尔维亚",
    "bulgaria": "保加利亚",
    "slovakia": "斯洛伐克",
    "slovenia": "斯洛文尼亚",
    "lithuania": "立陶宛",
    "latvia": "拉脱维亚",
    "estonia": "爱沙尼亚",
    "moldova": "摩尔多瓦",
    "bosnia and herzegovina": "波黑", "bosnia": "波黑",
    "albania": "阿尔巴尼亚",
    "north macedonia": "北马其顿", "macedonia": "北马其顿",
    "montenegro": "黑山",
    "kosovo": "科索沃",
    "georgia": "格鲁吉亚",
    "armenia": "亚美尼亚",
    "azerbaijan": "阿塞拜疆",
    "cyprus": "塞浦路斯",
    "malta": "马耳他",
    "iceland": "冰岛",
    "luxembourg": "卢森堡",
    "monaco": "摩纳哥",
    "liechtenstein": "列支敦士登",
    "andorra": "安道尔",
    "san marino": "圣马力诺",
    "vatican": "梵蒂冈",
    "qatar": "卡塔尔",
    "bahrain": "巴林",
    "kuwait": "科威特",
    "oman": "阿曼",
    "yemen": "也门",
    "syria": "叙利亚",
    "lebanon": "黎巴嫩",
    "jordan": "约旦",
    "palestine": "巴勒斯坦",
    "saudi arabia": "沙特阿拉伯",
    "united arab emirates": "阿联酋", "uae": "阿联酋",
    "azerbaijan": "阿塞拜疆",
    "brunei": "文莱",
    "macau": "中国澳门", "macao": "中国澳门",
    "taiwan": "中国台湾",
    "hong kong": "中国香港",
    "scotland": "英国", "wales": "英国", "northern ireland": "英国",
    "prc": "中国", "people's republic of china": "中国", "china": "中国",
}

# Special name patterns -> country
NAME_PATTERNS = [
    (r",?\s*USA$", "美国"),
    (r",?\s*United States$", "美国"),
    (r",?\s*US$", "美国"),
    (r",?\s*UK$", "英国"),
    (r",?\s*United Kingdom$", "英国"),
    (r",?\s*China$", "中国"),
    (r",?\s*Japan$", "日本"),
    (r",?\s*India$", "印度"),
    (r",?\s*Germany$", "德国"),
    (r",?\s*France$", "法国"),
    (r",?\s*Canada$", "加拿大"),
    (r",?\s*Australia$", "澳大利亚"),
    (r",?\s*Italy$", "意大利"),
    (r",?\s*Spain$", "西班牙"),
    (r",?\s*Russia$", "俄罗斯"),
    (r",?\s*South Korea$", "韩国"),
    (r"University of Tokyo", "日本"),
    (r"University of Oxford", "英国"),
    (r"University of Cambridge", "英国"),
    (r"ETH Zurich", "瑞士"),
    (r"Imperial College London", "英国"),
    (r"National University of Singapore", "新加坡"),
    (r"Tsinghua University", "中国"),
    (r"Peking University", "中国"),
]


def connect(cfg):
    return pymysql.connect(cursorclass=DictCursor, **cfg)


def normalize(name: str) -> str:
    return (name or "").strip()


def classify_tag(name: str) -> Tuple[Optional[str], Optional[str]]:
    """Return (country, region) for a tag name. Either may be None."""
    name = normalize(name)
    if not name or name in IGNORED_TAGS:
        return None, None
    if name in CONTINENTS:
        return None, name
    if name in COUNTRY_TO_CONTINENT:
        return name, COUNTRY_TO_CONTINENT[name]
    return None, None


def infer_from_name(name_zh: Optional[str], name_en: Optional[str]) -> Tuple[Optional[str], Optional[str]]:
    """Try to infer country from university name."""
    for pattern, country in NAME_PATTERNS:
        if name_en and re.search(pattern, name_en, re.IGNORECASE):
            return country, COUNTRY_TO_CONTINENT.get(country)
    # Chinese name -> China
    if name_zh and re.search(r"[\u4e00-\u9fff]", name_zh):
        return "中国", "亚洲"
    # Alias in full english name
    if name_en:
        lower = name_en.lower()
        for alias, country in COUNTRY_ALIASES.items():
            if alias in lower:
                return country, COUNTRY_TO_CONTINENT.get(country)
    return None, None


def load_menggy_tag_maps(conn) -> Tuple[Dict[str, List[str]], Dict[str, List[str]]]:
    """Return url_id -> list of country candidates, url_id -> list of region candidates."""
    country_map: Dict[str, List[str]] = {}
    region_map: Dict[str, List[str]] = {}
    with conn.cursor() as cur:
        cur.execute("""
            SELECT u.url_id, t.name_zh
            FROM university_tags ut
            JOIN universities u ON ut.university_id = u.id
            JOIN tags t ON ut.tag_id = t.id
        """)
        for row in cur.fetchall():
            uid = row["url_id"].lower().strip()
            name = normalize(row["name_zh"])
            country, region = classify_tag(name)
            if country:
                country_map.setdefault(uid, []).append(country)
            if region:
                region_map.setdefault(uid, []).append(region)
    return country_map, region_map


def pick_first(candidates: List[str]) -> Optional[str]:
    # Prefer real country over fallback; already filtered
    return candidates[0] if candidates else None


def main():
    src = connect(SOURCE)
    tgt = connect(TARGET)

    country_map, region_map = load_menggy_tag_maps(src)

    updates = []
    with tgt.cursor() as cur:
        cur.execute("SELECT url_id, name_zh, name_en, country, region FROM university WHERE deleted = 0")
        for row in cur.fetchall():
            uid = row["url_id"]
            name_zh = normalize(row["name_zh"])
            name_en = normalize(row["name_en"])
            current_country = normalize(row["country"])
            current_region = normalize(row["region"])

            new_country = current_country if current_country and current_country != "unknown" else None
            new_region = current_region if current_region and current_region != "unknown" else None

            # 1. Use menggy tags
            if not new_country:
                new_country = pick_first(country_map.get(uid, []))
            if not new_region:
                new_region = pick_first(region_map.get(uid, []))

            # 2. Derive region from newly found country
            if new_country and not new_region:
                new_region = COUNTRY_TO_CONTINENT.get(new_country)

            # 3. Name heuristics
            if not new_country:
                inferred_country, inferred_region = infer_from_name(name_zh, name_en)
                if inferred_country:
                    new_country = inferred_country
                if inferred_region:
                    new_region = inferred_region

            # 4. If only region known, we still lack country -> keep unknown for now
            #    (will be handled by fallback if needed)

            # 5. Fallback to avoid empty values
            if not new_country:
                new_country = "其他"
            if not new_region:
                new_region = "其他"

            if new_country != current_country or new_region != current_region:
                updates.append((new_country, new_region, uid))

    if updates:
        with tgt.cursor() as cur:
            cur.executemany("UPDATE university SET country = %s, region = %s WHERE url_id = %s", updates)
            tgt.commit()

    logger.info("Updated %d universities", len(updates))

    # Log remaining unknowns (should be 0)
    with tgt.cursor() as cur:
        cur.execute("SELECT COUNT(*) AS c FROM university WHERE deleted = 0 AND (country = 'unknown' OR region = 'unknown' OR country IS NULL OR region IS NULL)")
        remaining = cur.fetchone()["c"]
        cur.execute("SELECT COUNT(*) AS c FROM university WHERE deleted = 0 AND country = '其他'")
        fallback = cur.fetchone()["c"]
        logger.info("Remaining unknown: %d, fallback '其他': %d", remaining, fallback)

    src.close()
    tgt.close()


if __name__ == "__main__":
    main()
