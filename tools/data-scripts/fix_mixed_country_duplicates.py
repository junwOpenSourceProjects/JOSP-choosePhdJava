#!/usr/bin/env python3
"""
Fix remaining mixed-country duplicate university groups by inferring correct country
from url_id / english name keywords, then merge same-country duplicates.
"""
import logging
import re
from collections import defaultdict
from typing import Dict, List, Optional, Tuple

import pymysql
from pymysql.cursors import DictCursor

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")
logger = logging.getLogger(__name__)

TARGET = {"host": "localhost", "port": 3306, "user": "root", "password": "", "database": "choosephd", "charset": "utf8mb4"}


def connect():
    return pymysql.connect(cursorclass=DictCursor, **TARGET)


def normalize(text: Optional[str]) -> str:
    return (text or "").strip()


# Country -> continent
COUNTRY_TO_REGION = {
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
    "英国": "欧洲", "爱尔兰": "欧洲", "法国": "欧洲", "荷兰": "欧洲", "比利时": "欧洲", "卢森堡": "欧洲",
    "德国": "欧洲", "奥地利": "欧洲", "瑞士": "欧洲", "波兰": "欧洲", "捷克": "欧洲", "斯洛伐克": "欧洲",
    "匈牙利": "欧洲", "罗马尼亚": "欧洲", "保加利亚": "欧洲", "塞尔维亚": "欧洲", "克罗地亚": "欧洲",
    "斯洛文尼亚": "欧洲", "黑山": "欧洲", "北马其顿": "欧洲", "波斯尼亚和黑塞哥维那": "欧洲", "阿尔巴尼亚": "欧洲", "希腊": "欧洲",
    "意大利": "欧洲", "西班牙": "欧洲", "葡萄牙": "欧洲", "马耳他": "欧洲", "安道尔": "欧洲", "圣马力诺": "欧洲",
    "梵蒂冈": "欧洲", "摩纳哥": "欧洲", "列支敦士登": "欧洲", "丹麦": "欧洲", "挪威": "欧洲", "瑞典": "欧洲",
    "芬兰": "欧洲", "冰岛": "欧洲", "爱沙尼亚": "欧洲", "拉脱维亚": "欧洲", "立陶宛": "欧洲",
    "白俄罗斯": "欧洲", "俄罗斯": "欧洲", "乌克兰": "欧洲", "摩尔多瓦": "欧洲",
    "美国": "北美洲", "加拿大": "北美洲", "墨西哥": "北美洲", "危地马拉": "北美洲", "伯利兹": "北美洲",
    "萨尔瓦多": "北美洲", "洪都拉斯": "北美洲", "尼加拉瓜": "北美洲", "哥斯达黎加": "北美洲", "巴拿马": "北美洲",
    "古巴": "北美洲", "牙买加": "北美洲", "海地": "北美洲", "多米尼加": "北美洲", "巴哈马": "北美洲",
    "巴巴多斯": "北美洲", "特立尼达和多巴哥": "北美洲", "格林纳达": "北美洲", "圣卢西亚": "北美洲",
    "圣文森特和格林纳丁斯": "北美洲", "安提瓜和巴布达": "北美洲", "圣基茨和尼维斯": "北美洲", "多米尼克": "北美洲",
    "波多黎各": "北美洲",
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
    "巴西": "南美洲", "阿根廷": "南美洲", "智利": "南美洲", "乌拉圭": "南美洲", "巴拉圭": "南美洲",
    "玻利维亚": "南美洲", "秘鲁": "南美洲", "哥伦比亚": "南美洲", "委内瑞拉": "南美洲", "厄瓜多尔": "南美洲",
    "圭亚那": "南美洲", "苏里南": "南美洲", "法属圭亚那": "南美洲",
    "澳大利亚": "大洋洲", "新西兰": "大洋洲", "巴布亚新几内亚": "大洋洲", "斐济": "大洋洲", "所罗门群岛": "大洋洲",
    "萨摩亚": "大洋洲", "汤加": "大洋洲", "瓦努阿图": "大洋洲", "基里巴斯": "大洋洲", "瑙鲁": "大洋洲",
    "图瓦卢": "大洋洲", "帕劳": "大洋洲", "密克罗尼西亚": "大洋洲", "马绍尔群岛": "大洋洲",
}

# url_id / name_en keyword -> country
# NOTE: order matters - more specific / USA keywords checked first
COUNTRY_KEYWORDS = {
    "美国": ["new-york-university", "polytechnic-institute-new-york", "new-mexico", "usa", "united-states"],
    "加拿大": ["canada", "calgary", "ottawa", "toronto", "montreal", "vancouver", "quebec", "saskatchewan", "manitoba",
              "newfoundland", "new-brunswick", "prince-edward-island", "nova-scotia", "alberta", "british-columbia",
              "ontario", "universite-de", "laval", "guelph", "windsor", "winnipeg", "regina", "sherbrooke",
              "lethbridge", "york-university", "memorial-university", "ecole-de-technologie", "emily-carr"],
    "中国台湾": ["taiwan", "taipei", "taichung", "tainan", "kaohsiung", "ntu", "national-taiwan"],
    "古巴": ["cuba", "habana", "havana", "echeverria"],
    "哥斯达黎加": ["costa-rica", "tecnologico-de-costa-rica", "ulacit"],
    "多米尼加": ["santo-domingo", "intec"],
    "沙特阿拉伯": ["tabuk", "hail", "jeddah", "dammam", "saudi"],
    "巴拿马": ["panama", "panam-"],
    "英国": ["brighton", "hult-ashridge", "university-of-brighton"],
    "乌克兰": ["ukraine", "odessa", "odesa"],
    "墨西哥": ["mexico", "nuevo-leon", "sinaloa", "veracruz", "monterrey", "zulia", "autonomous-university"],
    "智利": ["chile", "valparaiso", "valpara-so"],
    "洪都拉斯": ["honduras", "unah"],
    "波多黎各": ["puerto-rico"],
    "瑞士": ["svizzera-italiana", "usi-universita"],
    "比利时": ["solvay-brussels", "ulb"],
    "奥地利": ["salzburg", "paris-lodron"],
    "爱尔兰": ["dublin", "trinity-college"],
    "罗马尼亚": ["bucharest", "politehnica", "suceava", "stefan-cel-mare"],
}


def infer_country_from_url(url_id: str, name_en: str) -> Optional[str]:
    uid = normalize(url_id).lower()
    ne = normalize(name_en).lower()
    text = uid + " " + ne
    for country, keywords in COUNTRY_KEYWORDS.items():
        for kw in keywords:
            if kw in text:
                return country
    return None


def choose_canonical(url_ids: List[str], ranking_counts: Dict[str, int]) -> str:
    def sort_key(u: str) -> Tuple[int, int, str]:
        return (-ranking_counts.get(u, 0), len(u), u)
    return sorted(url_ids, key=sort_key)[0]


def main(dry_run: bool = False):
    conn = connect()
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT url_id, name_zh, name_en, country FROM university WHERE deleted = 0 AND name_zh IS NOT NULL AND name_zh != ''")
            rows = cur.fetchall()

        groups: Dict[str, List[Tuple[str, str, str, str]]] = defaultdict(list)
        for r in rows:
            groups[normalize(r["name_zh"])].append((r["url_id"], normalize(r["name_en"]), normalize(r["country"]), r["url_id"]))

        mixed_groups = {name: items for name, items in groups.items() if len({c for _, _, c, _ in items}) > 1}

        country_fixes = []
        merge_groups = []

        for name, items in mixed_groups.items():
            inferred = {}
            for uid, ne, country, _ in items:
                ic = infer_country_from_url(uid, ne)
                if ic:
                    inferred[uid] = ic

            # Determine most common inferred country among the group
            votes = defaultdict(int)
            for uid, ic in inferred.items():
                votes[ic] += 1
            best_country = max(votes, key=lambda k: votes[k]) if votes else None

            if best_country:
                # Fix any member whose inferred country differs
                for uid, ne, country, _ in items:
                    ic = inferred.get(uid)
                    if ic and ic != country:
                        country_fixes.append((uid, ic))
                        logger.info("Fix country %s: %s -> %s (%s)", uid, country, ic, name)

                # After fixes, group by target country and plan merges
                target_groups: Dict[str, List[str]] = defaultdict(list)
                for uid, ne, country, _ in items:
                    target = inferred.get(uid) or country
                    target_groups[target].append(uid)
                for target_country, uids in target_groups.items():
                    if len(uids) > 1:
                        merge_groups.append((target_country, uids))
            else:
                logger.warning("No inferred country for group: %s", name)

        if not dry_run:
            with conn.cursor() as cur:
                for uid, country in country_fixes:
                    region = COUNTRY_TO_REGION[country]
                    cur.execute("UPDATE university SET country = %s, region = %s WHERE url_id = %s", (country, region, uid))
                conn.commit()

        logger.info("Fixed %d country tags", len(country_fixes))

        # Merge same-country duplicates within mixed groups
        merged = deleted = aliases = ranking_moved = 0
        if not dry_run:
            for target_country, uids in merge_groups:
                with conn.cursor() as cur:
                    cur.execute(
                        "SELECT university_id, COUNT(*) AS c FROM ranking_entry WHERE deleted=0 AND university_id IN (%s) GROUP BY university_id" % ",".join(["%s"] * len(uids)),
                        tuple(uids)
                    )
                    ranking_counts = {row["university_id"]: row["c"] for row in cur.fetchall()}

                canonical = choose_canonical(uids, ranking_counts)
                others = [u for u in uids if u != canonical]
                logger.info("Merging %s (%s) -> %s, dupes: %s", name, target_country, canonical, others)

                if dry_run:
                    continue

                with conn.cursor() as cur:
                    for other in others:
                        cur.execute("SELECT id, source_id, subject_id, year, rank_value FROM ranking_entry WHERE deleted=0 AND university_id = %s", (other,))
                        entries = cur.fetchall()
                        for e in entries:
                            cur.execute(
                                "SELECT id, rank_value FROM ranking_entry WHERE deleted=0 AND university_id = %s AND source_id = %s AND subject_id <=> %s AND year = %s",
                                (canonical, e["source_id"], e["subject_id"], e["year"])
                            )
                            existing = cur.fetchone()
                            if existing:
                                e_val = e["rank_value"] if e["rank_value"] is not None else 999999
                                ex_val = existing["rank_value"] if existing["rank_value"] is not None else 999999
                                if e_val < ex_val:
                                    cur.execute("UPDATE ranking_entry SET deleted=1 WHERE id = %s", (existing["id"],))
                                    cur.execute("UPDATE ranking_entry SET university_id = %s WHERE id = %s", (canonical, e["id"]))
                                    ranking_moved += 1
                                else:
                                    cur.execute("UPDATE ranking_entry SET deleted=1 WHERE id = %s", (e["id"],))
                            else:
                                cur.execute("UPDATE ranking_entry SET university_id = %s WHERE id = %s", (canonical, e["id"]))
                                ranking_moved += 1

                        cur.execute(
                            "INSERT INTO university_alias (alias_url_id, target_url_id) VALUES (%s, %s) ON DUPLICATE KEY UPDATE target_url_id = VALUES(target_url_id)",
                            (other, canonical)
                        )
                        cur.execute("UPDATE university SET deleted=1 WHERE url_id = %s", (other,))
                        aliases += 1
                        deleted += 1
                    merged += 1

            if merge_groups:
                conn.commit()

        logger.info("Merged %d groups, deleted %d, aliases %d, moved %d ranking entries", merged, deleted, aliases, ranking_moved)

    finally:
        conn.close()


if __name__ == "__main__":
    import sys
    dry = "--dry-run" in sys.argv
    main(dry_run=dry)
