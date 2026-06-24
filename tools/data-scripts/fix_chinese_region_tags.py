#!/usr/bin/env python3
"""
Audit and fix country/region tags for Chinese universities.
Ensure mainland universities are '中国', Taiwan universities are '中国台湾',
Hong Kong universities are '中国香港', Macau universities are '中国澳门'.
Only processes universities currently tagged as one of these four regions.
"""
import logging
import re
from typing import Optional

import pymysql
from pymysql.cursors import DictCursor

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")
logger = logging.getLogger(__name__)

TARGET = {"host": "localhost", "port": 3306, "user": "root", "password": "", "database": "choosephd", "charset": "utf8mb4"}

CHINESE_REGIONS = {"中国", "中国台湾", "中国香港", "中国澳门"}


def connect():
    return pymysql.connect(cursorclass=DictCursor, **TARGET)


def normalize(text: Optional[str]) -> str:
    return (text or "").strip()


# Explicit force lists ---------------------------------------------------------

# Chinese name -> expected country
EXPLICIT_NAME_FIXES = {
    # Taiwan
    "中央研究院": "中国台湾",
    "国防医学大学": "中国台湾",
    "国立宜兰大学": "中国台湾",
    "宜兰大学": "中国台湾",
    "弘光大学": "中国台湾",
    "弘光科技大学": "中国台湾",
    "鸿光大学": "中国台湾",
    "辅英大学": "中国台湾",
    "长庚大学": "中国台湾",
    "长庚科技大学": "中国台湾",
    "中山医学大学": "中国台湾",
    "慈济大学": "中国台湾",
    "慈济科技大学": "中国台湾",
    "元智大学": "中国台湾",
    "佛光大学": "中国台湾",
    "世新大学": "中国台湾",
    "东吴大学": "中国台湾",
    "东海大学，台湾": "中国台湾",
    "东海大学": "中国台湾",
    "中国文化大学": "中国台湾",
    "中华大学": "中国台湾",
    "南华大学，台湾": "中国台湾",
    "中原大学": "中国台湾",
    "中正大学": "中国台湾",
    "中兴大学": "中国台湾",
    "义守大学": "中国台湾",
    "亚洲大学，台中": "中国台湾",
    "大叶大学": "中国台湾",
    "华梵大学": "中国台湾",
    "玄奘大学": "中国台湾",
    "开南大学": "中国台湾",
    "康宁大学": "中国台湾",
    "马偕大学": "中国台湾",
    "马偕医学院": "中国台湾",
    "真理大学": "中国台湾",
    "实践大学": "中国台湾",
    "静宜大学": "中国台湾",
    "逢甲大学": "中国台湾",
    "朝阳科技大学": "中国台湾",
    "朝阳大学": "中国台湾",
    "明志科技大学": "中国台湾",
    "南台科技大学": "中国台湾",
    "稻江科技暨管理学院": "中国台湾",
    "台东大学": "中国台湾",
    "国立台东大学": "中国台湾",
    "花莲教育大学": "中国台湾",
    "澎湖科技大学": "中国台湾",
    "金门大学": "中国台湾",
    "国立金门大学": "中国台湾",
    "联合大学（台湾）": "中国台湾",
    "国立联合大学": "中国台湾",
    "国立台北大学": "中国台湾",
    "国立台中教育大学": "中国台湾",
    "国立台南大学": "中国台湾",
    "国立高雄大学": "中国台湾",
    "国立高雄师范大学": "中国台湾",
    "国立屏东大学": "中国台湾",
    "国立彰化师范大学": "中国台湾",
    "国立云林科技大学": "中国台湾",
    "国立东华大学": "中国台湾",
    "国立中兴大学": "中国台湾",
    "国立中正大学": "中国台湾",
    "国立中山大学": "中国台湾",
    "国立台湾大学": "中国台湾",
    "国立清华大学": "中国台湾",
    "国立交通大学": "中国台湾",
    "国立成功大学": "中国台湾",
    "国立政治大学": "中国台湾",
    "国立台湾科技大学": "中国台湾",
    "国立台湾师范大学": "中国台湾",
    "国立台湾海洋大学": "中国台湾",
    "台北医学大学": "中国台湾",
    "台北市立大学": "中国台湾",
    "台北科技大学": "中国台湾",
    "国立台北科技大学": "中国台湾",
    "台中科技大学": "中国台湾",
    "国立台中科技大学": "中国台湾",
    "台南大学": "中国台湾",
    "勤益科技大学": "中国台湾",
    "国立勤益科技大学": "中国台湾",
    "台湾大学": "中国台湾",
    "台湾科技大学": "中国台湾",
    "台湾师范大学": "中国台湾",
    "台湾艺术大学": "中国台湾",
    "台湾海洋大学": "中国台湾",
    "高雄医学大学": "中国台湾",
    "高雄科技大学": "中国台湾",
    "淡江大学": "中国台湾",
    "铭传大学": "中国台湾",
    "辅仁大学": "中国台湾",
    "台湾中原大学": "中国台湾",
    "台湾师范大学": "中国台湾",
    "台湾中兴大学": "中国台湾",
    "台湾中正大学": "中国台湾",
    "台湾中山大学": "中国台湾",
    "台湾东华大学": "中国台湾",
    "台湾义守大学": "中国台湾",
    "台湾云林科技大学": "中国台湾",
    "台湾屏东科技大学": "中国台湾",
    "台湾海洋大学": "中国台湾",
    "台湾暨南国际大学": "中国台湾",
    "台湾嘉义大学": "中国台湾",
    "台湾联合大学": "中国台湾",
    "台湾警察专科学校": "中国台湾",

    # Hong Kong
    "香港中文大学": "中国香港",
    "香港大学": "中国香港",
    "香港科技大学": "中国香港",
    "香港理工大学": "中国香港",
    "香港城市大学": "中国香港",
    "香港浸会大学": "中国香港",
    "香港教育大学": "中国香港",
    "香港都会大学": "中国香港",
    "香港恒生大学": "中国香港",
    "香港树仁大学": "中国香港",
    "香港演艺学院": "中国香港",
    "香港演艺学院 (HKAPA)": "中国香港",
    "香港岭南大学": "中国香港",
    "东华学院": "中国香港",
    "香港高等科技教育学院": "中国香港",
    "珠海学院": "中国香港",
    "香港专业教育学院": "中国香港",
    "香港知专设计学院": "中国香港",
    "香港珠海学院": "中国香港",

    # Macau
    "澳门大学": "中国澳门",
    "澳门科技大学": "中国澳门",
    "澳门理工大学": "中国澳门",
    "澳门旅游大学（前澳门旅游学院）": "中国澳门",
    "澳门旅游学院": "中国澳门",
    "圣若瑟大学，澳门": "中国澳门",
    "圣若瑟大学": "中国澳门",
    "澳门城市大学": "中国澳门",

    # Mainland joint campuses / should be 中国
    "香港中文大学，深圳": "中国",
    "香港科技大学（广州）": "中国",
    "北京师范大学-香港浸会大学联合国际学院": "中国",
    "北师香港浸会大学": "中国",
    "香港大学-复旦大学管理学院": "中国",
    "香港城市大学（东莞）": "中国",
    "香港理工大学（佛山）": "中国",
    "香港科技大学（佛山）": "中国",
}

# url_id -> expected country
EXPLICIT_URL_FIXES = {
    "academia-sinica": "中国台湾",
    "national-defense-medical-center": "中国台湾",
    "beijing-jiaotong-university": "中国",
    "shanghai-jiao-tong-university": "中国",
    "xian-jiaotong-university": "中国",
    "southwest-jiaotong-university": "中国",
    "dalian-jiaotong-university": "中国",
    "lanzhou-jiaotong-university": "中国",
    "east-china-jiaotong-university": "中国",
    "chongqing-jiaotong-university": "中国",
    "chinese-university-hong-kong-shenzhen": "中国",
    "hong-kong-university-science-and-technology-guangzhou": "中国",
    "beijing-normal-university-hong-kong-baptist-university-united-international-college": "中国",
    "the-beijing-normal–hong-kong-baptist-university": "中国",
    "university-of-hong-kong-fudan-university-school-of-management": "中国",
    "tung-wah-college": "中国香港",
    "macao-polytechnic-university": "中国澳门",
    "university-south-china": "中国",
    "chaoyang-normal-university": "中国",
    "beijing-union-university": "中国",
    "guangdong-ocean-university": "中国",
    "chang-gung-university-of-science-and-technology": "中国台湾",
    "chung-shan-medical-university": "中国台湾",
    "tzu-chi-university-science-and-technology": "中国台湾",
    "fooyin-university": "中国台湾",
    "hungkuang-university": "中国台湾",
    "national-ilan-university": "中国台湾",
    "chang-jung-christian-university": "中国台湾",
    "ling-tung-university": "中国台湾",
}


# Mainland cities / provinces that strongly indicate 中国
MAINLAND_CITIES = {
    "上海", "北京", "天津", "重庆", "深圳", "广州", "东莞", "珠海", "佛山",
    "河北", "山西", "辽宁", "吉林", "黑龙江", "江苏", "浙江", "安徽", "福建", "江西", "山东",
    "河南", "湖北", "湖南", "广东", "海南", "四川", "贵州", "云南", "陕西", "甘肃", "青海",
    "内蒙古", "广西", "西藏", "宁夏", "新疆", "雄安",
    "兰州", "西安", "南京", "杭州", "合肥", "福州", "南昌", "济南", "郑州", "武汉", "长沙",
    "成都", "贵阳", "昆明", "海口", "三亚", "沈阳", "大连", "长春", "哈尔滨",
    "石家庄", "太原", "呼和浩特", "乌鲁木齐", "银川", "西宁", "拉萨", "南宁", "桂林", "柳州",
    "无锡", "苏州", "宁波", "温州", "青岛", "烟台", "厦门", "泉州", "徐州", "绍兴", "嘉兴",
    "保定", "潍坊", "南通", "扬州", "湖州", "金华", "淄博", "威海", "中山", "惠州", "江门",
    "汕头", "湛江", "茂名", "肇庆", "韶关", "清远", "河源", "梅州", "揭阳", "阳江", "云浮",
    "株洲", "湘潭", "衡阳", "岳阳", "常德", "邵阳", "益阳", "郴州", "永州", "怀化", "娄底",
    "宜昌", "襄阳", "荆州", "黄冈", "孝感", "黄石", "十堰", "荆门", "鄂州", "咸宁", "随州",
    "恩施", "洛阳", "开封", "安阳", "新乡", "许昌", "平顶山", "焦作", "商丘", "信阳", "周口",
    "驻马店", "南阳", "濮阳", "漯河", "鹤壁", "三门峡", "济源",
    "烟台", "济宁", "临沂", "淄博", "潍坊", "泰安", "威海", "德州", "聊城", "滨州", "菏泽",
    "枣庄", "日照", "莱芜",
    "常州", "无锡", "苏州", "南通", "扬州", "盐城", "徐州", "淮安", "镇江", "连云港", "泰州", "宿迁",
    "昆山", "江阴", "张家港", "常熟", "宜兴", "太仓",
    "温州", "宁波", "嘉兴", "湖州", "绍兴", "金华", "衢州", "舟山", "台州", "丽水",
    "芜湖", "蚌埠", "淮南", "马鞍山", "淮北", "铜陵", "安庆", "黄山", "滁州", "阜阳", "宿州", "六安", "亳州", "池州", "宣城",
    "福州", "厦门", "莆田", "三明", "泉州", "漳州", "南平", "龙岩", "宁德",
    "南昌", "景德镇", "萍乡", "九江", "新余", "鹰潭", "赣州", "吉安", "宜春", "抚州", "上饶",
    "济南", "青岛", "淄博", "枣庄", "东营", "烟台", "潍坊", "济宁", "泰安", "威海", "日照", "莱芜", "临沂", "德州", "聊城", "滨州", "菏泽",
    "郑州", "开封", "洛阳", "平顶山", "安阳", "鹤壁", "新乡", "焦作", "濮阳", "许昌", "漯河", "三门峡", "南阳", "商丘", "信阳", "周口", "驻马店", "济源",
    "武汉", "黄石", "十堰", "宜昌", "襄阳", "鄂州", "荆门", "孝感", "荆州", "黄冈", "咸宁", "随州", "恩施",
    "长沙", "株洲", "湘潭", "衡阳", "邵阳", "岳阳", "常德", "张家界", "益阳", "郴州", "永州", "怀化", "娄底", "湘西",
    "广州", "韶关", "深圳", "珠海", "汕头", "佛山", "江门", "湛江", "茂名", "肇庆", "惠州", "梅州", "汕尾", "河源", "阳江", "清远", "东莞", "中山", "潮州", "揭阳", "云浮",
    "南宁", "柳州", "桂林", "梧州", "北海", "防城港", "钦州", "贵港", "玉林", "百色", "贺州", "河池", "来宾", "崇左",
    "海口", "三亚", "三沙", "儋州",
    "成都", "自贡", "攀枝花", "泸州", "德阳", "绵阳", "广元", "遂宁", "内江", "乐山", "南充", "眉山", "宜宾", "广安", "达州", "雅安", "巴中", "资阳", "阿坝", "甘孜", "凉山",
    "贵阳", "六盘水", "遵义", "安顺", "毕节", "铜仁", "黔西南", "黔东南", "黔南",
    "昆明", "曲靖", "玉溪", "保山", "昭通", "丽江", "普洱", "临沧", "楚雄", "红河", "文山", "西双版纳", "大理", "德宏", "怒江", "迪庆",
    "拉萨", "日喀则", "昌都", "林芝", "山南", "那曲", "阿里",
    "西安", "铜川", "宝鸡", "咸阳", "渭南", "延安", "汉中", "榆林", "安康", "商洛",
    "兰州", "嘉峪关", "金昌", "白银", "天水", "武威", "张掖", "平凉", "酒泉", "庆阳", "定西", "陇南", "临夏", "甘南",
    "西宁", "海东", "海北", "黄南", "海南", "果洛", "玉树", "海西",
    "银川", "石嘴山", "吴忠", "固原", "中卫",
    "乌鲁木齐", "克拉玛依", "吐鲁番", "哈密", "昌吉", "博尔塔拉", "巴音郭楞", "阿克苏", "克孜勒苏", "喀什", "和田", "伊犁", "塔城", "阿勒泰",
    "呼和浩特", "包头", "乌海", "赤峰", "通辽", "鄂尔多斯", "呼伦贝尔", "巴彦淖尔", "乌兰察布", "兴安", "锡林郭勒", "阿拉善",
    "石家庄", "唐山", "秦皇岛", "邯郸", "邢台", "保定", "张家口", "承德", "沧州", "廊坊", "衡水",
    "太原", "大同", "阳泉", "长治", "晋城", "朔州", "晋中", "运城", "忻州", "临汾", "吕梁",
    "沈阳", "大连", "鞍山", "抚顺", "本溪", "丹东", "锦州", "营口", "阜新", "辽阳", "盘锦", "铁岭", "朝阳", "葫芦岛",
    "长春", "吉林", "四平", "辽源", "通化", "白山", "松原", "白城", "延边", "长白山",
    "哈尔滨", "齐齐哈尔", "鸡西", "鹤岗", "双鸭山", "大庆", "伊春", "佳木斯", "七台河", "牡丹江", "黑河", "绥化", "大兴安岭",
}

# Taiwan-only indicators (names not typically mainland)
TAIWAN_ONLY_NAMES = {
    "世新", "东吴", "东海", "中国文化", "元智", "佛光", "南华", "中原大学", "中正", "中兴",
    "义守", "云林", "高雄", "彰化", "屏东", "嘉义", "淡江", "铭传", "实践", "静宜", "逢甲",
    "长庚", "慈济", "大叶", "华梵", "玄奘", "开南", "康宁", "马偕", "真理", "辅仁", "朝阳",
    "明志", "南台", "稻江", "台东", "花莲", "澎湖", "金门", "联合大学",
    "国立台北", "国立台中", "国立台南", "国立高雄", "国立屏东", "国立彰化", "国立云林",
    "国立东华", "国立中兴", "国立中正", "国立中山", "国立台湾", "国立中央", "国立清华",
    "国立交通", "国立成功", "国立政治", "国立台湾大学", "国立清华大学", "国立交通大学",
    "台湾大学", "台湾清华", "台湾科技", "台湾师范", "台湾艺术", "台湾海洋", "台湾医",
    "台北医学", "台北市立", "台北科技", "台中科技", "台南", "勤益科技", "亚洲大学，台中",
    "大叶大学", "中华大学", "中台", "中山医学", "元智", "台北城市", "台东", "台塑",
}

TAIWAN_CITIES_EN = [
    "taipei", "taichung", "tainan", "kaohsiung", "hsinchu", "taoyuan",
    "hualien", "taitung", "chiayi", "changhua", "pingtung", "yunlin",
    "miaoli", "nantou", "keelung", "new-taipei", "ilan", "yilan",
]


def classify_chinese_region(name_zh: str, name_en: str, url_id: str) -> Optional[str]:
    """
    Return expected country for a Chinese-region university.
    """
    nz = normalize(name_zh)
    ne = normalize(name_en).lower()
    uid = normalize(url_id).lower()

    # 1. Explicit name fixes
    if nz in EXPLICIT_NAME_FIXES:
        return EXPLICIT_NAME_FIXES[nz]

    # 2. Explicit url_id fixes
    if uid in EXPLICIT_URL_FIXES:
        return EXPLICIT_URL_FIXES[uid]

    # 3. Mainland campus of HK/Macau/Taiwan universities
    if "深圳" in nz or "shenzhen" in uid or "guangzhou" in uid or "广州" in nz or "东莞" in uid or "横琴" in nz or "南沙" in nz or "佛山" in nz:
        return "中国"

    # 4. Macau
    if "澳门" in nz or "macau" in ne or "macao" in ne or "macau" in uid or "macao" in uid:
        return "中国澳门"

    # 5. Hong Kong (Chinese or English)
    if "香港" in nz or "hong kong" in ne or "hong-kong" in uid:
        return "中国香港"
    # English-only HK institutions without "Hong Kong" in displayed name
    hk_url_patterns = ["hku", "hkust", "cityuhk", "polyu", "cuhk", "hkbu", "lingu", "eduhk", "hsu", "syu", "hkapa", "uowchk", "vtc", "tung-wah"]
    for pat in hk_url_patterns:
        if pat in uid:
            return "中国香港"
    # Specific English names for HK institutions
    if any(x in ne for x in [
        "university of hong kong",
        "chinese university of hong kong",
        "hong kong university of science and technology",
        "hong kong polytechnic university",
        "city university of hong kong",
        "hong kong baptist university",
        "education university of hong kong",
        "hong kong metropolitan university",
        "hang seng university of hong kong",
        "hong kong shue yan university",
        "hong kong academy for performing arts",
        "lingnan university, hong kong",
        "tung wah college",
        "hong kong design institute",
    ]):
        return "中国香港"

    # 6. Taiwan by explicit markers
    if "台湾" in nz or "taiwan" in ne or "-taiwan" in uid or "taiwan-" in uid:
        return "中国台湾"

    for city in TAIWAN_CITIES_EN:
        if city in ne or city in uid:
            return "中国台湾"

    for only in TAIWAN_ONLY_NAMES:
        if only in nz:
            return "中国台湾"

    # 7. Mainland by city/province
    for city in MAINLAND_CITIES:
        if city in nz:
            return "中国"

    # 8. If currently tagged as Chinese region but no strong indicator, default to 中国
    if re.search(r"[\u4e00-\u9fff]", nz):
        return "中国"

    return None


def main(dry_run: bool = False):
    conn = connect()
    try:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT url_id, name_zh, name_en, country, region FROM university WHERE deleted = 0 AND country IN %s",
                (tuple(CHINESE_REGIONS),)
            )
            rows = cur.fetchall()

        updates = []
        suspicious = []

        for r in rows:
            uid = normalize(r["url_id"])
            nz = normalize(r["name_zh"])
            ne = normalize(r["name_en"])
            current_country = normalize(r["country"])
            current_region = normalize(r["region"])

            expected = classify_chinese_region(nz, ne, uid)
            if expected is None:
                continue

            expected_region = "亚洲"

            if expected != current_country or current_region != expected_region:
                updates.append((expected, expected_region, uid))
                logger.info("Fix %s: %s -> %s (%s)", uid, current_country, expected, nz)

        if not dry_run and updates:
            with conn.cursor() as cur:
                cur.executemany("UPDATE university SET country = %s, region = %s WHERE url_id = %s", updates)
                conn.commit()

        logger.info("Updated %d Chinese universities", len(updates))

        # Re-check
        with conn.cursor() as cur:
            cur.execute(
                "SELECT url_id, name_zh, name_en, country, region FROM university WHERE deleted = 0 AND country IN %s",
                (tuple(CHINESE_REGIONS),)
            )
            all_chinese = cur.fetchall()

        for r in all_chinese:
            uid = normalize(r["url_id"])
            nz = normalize(r["name_zh"])
            ne = normalize(r["name_en"])
            current_country = normalize(r["country"])
            expected = classify_chinese_region(nz, ne, uid)
            if expected and expected != current_country:
                suspicious.append((uid, nz, ne, current_country, expected))

        if suspicious:
            logger.warning("Remaining suspicious cases: %d", len(suspicious))
            for uid, nz, ne, cur, exp in suspicious[:30]:
                logger.warning("  %s | %s | %s | current=%s expected=%s", uid, nz, ne, cur, exp)
        else:
            logger.info("No suspicious Chinese region mappings found")

    finally:
        conn.close()


if __name__ == "__main__":
    import sys
    dry = "--dry-run" in sys.argv
    main(dry_run=dry)
