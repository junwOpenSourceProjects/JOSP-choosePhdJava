#!/usr/bin/env python3
"""为 ranking_source 补充中文名，将当前 name_zh（实际为英文）迁移到 name_en。"""

import os
import re
import mysql.connector

DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "",
    "database": "choosephd",
    "charset": "utf8mb4",
}

# 明确映射：英文名称 -> 中文名称
EXPLICIT = {
    "MENGGY WORLD UNIVERSITIES RANKINGS": "MENGGY 世界大学排名",
    "QS WORLD UNIVERSITIES RANKINGS": "QS 世界大学排名",
    "THE YOUNG UNIVERSITY RANKINGS": "泰晤士高等教育年轻大学排名",
    "THE WORLD UNIVERSITIES RANKINGS": "泰晤士高等教育世界大学排名",
    "QS SUSTAINABILITY UNIVERSITY RANKINGS": "QS 可持续发展大学排名",
    "QS GLOBAL MBA RANKINGS": "QS 全球 MBA 排名",
    "ARWU WORLD UNIVERSITIES RANKINGS": "软科世界大学学术排名",
    "ARWU CHINESE UNIVERSITIES RANKINGS": "软科中国大学排名",
    "RUR WORLD UNIVERSITY RANKINGS": "RUR 世界大学排名",
    "CWUR WORLD UNIVERSITIES RANKINGS": "CWUR 世界大学排名",
    "EDURANK WORLD UNIVERSITY RANKINGS": "EduRank 世界大学排名",
    "MOSIUR WORLD UNIVERSITY RANKING": "莫斯科国际大学排名（三项大学使命）",
    "MOSIUR BRICS UNIVERSITIES RANKING": "莫斯科国际大学金砖国家排名",
    "WU RANKINGS": "武书连中国大学排名",
    "XIAOYOUHUI CHINESE UNIVERSITY RANKINGS": "校友会中国大学排名",
    "USNEWS WORLD UNIVERSITIES RANKINGS": "U.S. News 世界大学排名",
    "USNEWS NATIONAL UNIVERSITY RANKINGS": "U.S. News 美国大学综合排名",
    "QS ASIA UNIVERSITY RANKINGS": "QS 亚洲大学排名",
    "THE ASIA UNIVERSITY RANKINGS": "泰晤士高等教育亚洲大学排名",
    "THE JAPAN UNIVERSITY RANKINGS": "泰晤士高等教育日本大学排名",
    "THE WORLD REPUTATION RANKINGS": "泰晤士高等教育世界大学声誉排名",
    "THE IMPACT RANKINGS": "泰晤士高等教育影响力排名",
    "QS GRADUATE EMPLOYABILITY RANKINGS": "QS 毕业生就业力排名",
    "FT MBA RANKINGS": "金融时报 MBA 排名",
    "FT EXECUTIVE MBA RANKINGS": "金融时报高管 MBA 排名",
    "FT ONLINE MBA RANKINGS": "金融时报在线 MBA 排名",
    "FT MASTERS IN MANAGEMENT RANKINGS": "金融时报管理学硕士排名",
    "FT MASTERS IN FINANCE PRE EXPERIENCE RANKINGS": "金融时报金融硕士（无经验）排名",
    "FT MASTERS IN FINANCE POST EXPERIENCE RANKINGS": "金融时报金融硕士（有经验）排名",
    "FT EXECUTIVE EDUCATION CUSTOM RANKINGS": "金融时报高管教育定制课程排名",
    "FT EXECUTIVE EDUCATION OPEN RANKINGS": "金融时报高管教育公开课程排名",
    "FT EUROPEAN BUSINESS SCHOOL RANKINGS": "金融时报欧洲商学院排名",
    "EDURANK AFRICA UNIVERSITY RANKINGS": "EduRank 非洲大学排名",
    "EDURANK ASIA UNIVERSITY RANKINGS": "EduRank 亚洲大学排名",
    "EDURANK OCEANIA UNIVERSITY RANKINGS": "EduRank 大洋洲大学排名",
    "EDURANK NORTH AMERICA UNIVERSITY RANKINGS": "EduRank 北美大学排名",
    "EDURANK LATIN AMERICA UNIVERSITY RANKINGS": "EduRank 拉丁美洲大学排名",
    "EDURANK EU UNIVERSITY RANKINGS": "EduRank 欧盟大学排名",
    "RUR WORLD UNIVERSITY ACADEMIC RANKINGS": "RUR 世界大学学术排名",
    "RUR WORLD UNIVERSITY REPUTATION RANKINGS": "RUR 世界大学声誉排名",
    "ARWU FOUR REGIONS ACROSS STRAIGHT RANKINGS": "软科两岸四地大学排名",
    "THE INTERDISCIPLINARY SCIENCE RANKINGS": "泰晤士高等教育跨学科科学排名",
}

# 模式替换：按顺序应用
PATTERNS = [
    # 趋势/增长/下降类前缀
    (r"^GROWTH TREND\s+", "增长趋势 "),
    (r"^DECLINING TREND\s+", "下降趋势 "),
    # 主体名称
    (r"\bQS\b", "QS"),
    (r"\bTHE\b", "泰晤士高等教育"),
    (r"\bARWU\b", "软科"),
    (r"\bUSNEWS\b", "U.S. News"),
    (r"\bCWUR\b", "CWUR"),
    (r"\bEDURANK\b", "EduRank"),
    (r"\bRUR\b", "RUR"),
    (r"\bMOSIUR\b", "莫斯科国际大学排名"),
    (r"\bFT\b", "金融时报"),
    (r"\bMENGGY\b", "MENGGY"),
    (r"\bXIAOYOUHUI\b", "校友会"),
    (r"\bWU RANKINGS\b", "武书连中国大学排名"),
    # 区域/类型后缀
    (r"\bWORLD UNIVERSITIES RANKINGS\b", "世界大学排名"),
    (r"\bWORLD UNIVERSITY RANKINGS\b", "世界大学排名"),
    (r"\bWORLD UNIVERSITY RANKING\b", "世界大学排名"),
    (r"\bUNIVERSITIES RANKING\b", "大学排名"),
    (r"\bUNIVERSITY RANKINGS\b", "大学排名"),
    (r"\bUNIVERSITY RANKING\b", "大学排名"),
    (r"\bNATIONAL UNIVERSITY RANKINGS\b", "美国大学综合排名"),
    (r"\bASIA UNIVERSITY RANKINGS\b", "亚洲大学排名"),
    (r"\bAFRICA UNIVERSITY RANKINGS\b", "非洲大学排名"),
    (r"\bOCEANIA UNIVERSITY RANKINGS\b", "大洋洲大学排名"),
    (r"\bNORTH AMERICA UNIVERSITY RANKINGS\b", "北美大学排名"),
    (r"\bLATIN AMERICA UNIVERSITY RANKINGS\b", "拉丁美洲大学排名"),
    (r"\bEU UNIVERSITY RANKINGS\b", "欧盟大学排名"),
    (r"\bBRICS UNIVERSITIES RANKING\b", "金砖国家大学排名"),
    (r"\bCHINESE UNIVERSITIES RANKINGS\b", "中国大学排名"),
    (r"\bJAPAN UNIVERSITY RANKINGS\b", "日本大学排名"),
    (r"\bYOUNG UNIVERSITY RANKINGS\b", "年轻大学排名"),
    (r"\bWORLD REPUTATION RANKINGS\b", "世界大学声誉排名"),
    (r"\bUNIVERSITY SUBJECT RANKINGS\b", "学科排名"),
    (r"\bUNIVERSITY SUBJECT RANKING\b", "学科排名"),
    (r"\bSUBJECT RANKINGS\b", "学科排名"),
    (r"\bIMPACT RANKINGS\b", "影响力排名"),
    (r"\bSUSTAINABILITY UNIVERSITY RANKINGS\b", "可持续发展大学排名"),
    (r"\bGRADUATE EMPLOYABILITY RANKINGS\b", "毕业生就业力排名"),
    (r"\bGLOBAL MBA RANKINGS\b", "全球 MBA 排名"),
    (r"\bMBA RANKINGS\b", "MBA 排名"),
    (r"\bEXECUTIVE MBA RANKINGS\b", "高管 MBA 排名"),
    (r"\bONLINE MBA RANKINGS\b", "在线 MBA 排名"),
    (r"\bMASTERS IN MANAGEMENT RANKINGS\b", "管理学硕士排名"),
    (r"\bMASTERS IN FINANCE PRE EXPERIENCE RANKINGS\b", "金融硕士（无经验）排名"),
    (r"\bMASTERS IN FINANCE POST EXPERIENCE RANKINGS\b", "金融硕士（有经验）排名"),
    (r"\bEXECUTIVE EDUCATION CUSTOM RANKINGS\b", "高管教育定制课程排名"),
    (r"\bEXECUTIVE EDUCATION OPEN RANKINGS\b", "高管教育公开课程排名"),
    (r"\bEUROPEAN BUSINESS SCHOOL RANKINGS\b", "欧洲商学院排名"),
    (r"\bFOUR REGIONS ACROSS STRAIGHT RANKINGS\b", "两岸四地大学排名"),
    (r"\bACADEMIC RANKINGS\b", "学术排名"),
    (r"\bREPUTATION RANKINGS\b", "声誉排名"),
    (r"\bINTERDISCIPLINARY SCIENCE RANKINGS\b", "跨学科科学排名"),
]

# 学科名称映射
SUBJECTS = {
    "LIFE SCIENCES": "生命科学",
    "ENGINEERING": "工程学",
    "EDUCATION": "教育学",
    "LAW": "法学",
    "BUSINESS ECONOMICS": "商业与经济",
    "PSYCHOLOGY": "心理学",
    "SOCIAL SCIENCES": "社会科学",
    "ARTS HUMANITIES": "艺术与人文",
    "PHYSICAL SCIENCES": "物理科学",
    "CLINICAL HEALTH": "临床与健康",
    "COMPUTER SCIENCE": "计算机科学",
    "CHEMICAL ENGINEERING": "化学工程",
    "AUTOMATION CONTROL": "自动化与控制",
    "CHEMISTRY": "化学",
    "BIOTECHNOLOGY": "生物技术",
    "AEROSPACE ENGINEERING": "航空航天工程",
    "AGRICULTURAL SCIENCES": "农业科学",
    "BIOMEDICAL ENGINEERING": "生物医学工程",
    "BUSINESS ADMINISTRATION": "工商管理",
    "BIOLOGICAL SCIENCES": "生物科学",
    "ARTIFICIAL INTELLIGENCE": "人工智能",
    "ATMOSPHERIC SCIENCE": "大气科学",
    "IMMUNOLOGY AND MICROBIOLOGY": "免疫学与微生物学",
    "TECHNICAL SCIENCES": "技术科学",
    "DENTISTRY": "牙医学",
    "PHARMACOLOGY TOXICOLOGY AND PHARMACEUTICS": "药理学、毒理学与药剂学",
    "ECONOMICS": "经济学",
    "VETERINARY": "兽医学",
    "EARTH AND PLANETARY SCIENCES": "地球与行星科学",
    "MATERIALS SCIENCE": "材料科学",
    "NEUROSCIENCE": "神经科学",
    "BIOCHEMISTRY GENETICS AND MOLECULAR BIOLOGY": "生物化学、遗传学与分子生物学",
    "MEDICINE": "医学",
    "MEDICAL SCIENCES": "医学科学",
    "NUCLEAR EDUCATION AND TECHNOLOGY": "核教育与技术",
    "LIFE SCIENCES MEDICINE": "生命科学与医学",
    "DECISION SCIENCES": "决策科学",
    "NANOSCIENCE AND NANOTECHNOLOGY": "纳米科学与纳米技术",
    "NATURAL SCIENCES": "自然科学",
    "BUSINESS MANAGEMENT AND ACCOUNTING": "商业、管理与会计",
    "BUSINESS MANAGEMENT STUDIES": "商业与管理研究",
    "NURSING": "护理学",
    "MATHEMATICS": "数学",
    "ENVIRONMENTAL SCIENCE": "环境科学",
    "ENERGY": "能源",
    "PHYSICS ASTRONOMY": "物理与天文学",
    "PHARMACY PHARMACOLOGY": "药学与药理学",
    "MODERN LANGUAGES": "现代语言",
    "LINGUISTICS": "语言学",
    "LAW AND LEGAL STUDIES": "法律与法律研究",
    "HISTORY": "历史",
    "GEOGRAPHY": "地理学",
    "GEOLOGY": "地质学",
    "GEOPHYSICS": "地球物理学",
    "CLASSICS ANCIENT HISTORY": "古典文学与古代史",
    "ENGLISH LANGUAGE AND LITERATURE": "英语语言与文学",
    "ARCHAEOLOGY": "考古学",
    "DEVELOPMENT STUDIES": "发展研究",
    "SOCIOLOGY": "社会学",
    "POLITICS": "政治学",
    "SOCIAL POLICY ADMINISTRATION": "社会政策与管理",
    "STATISTICS AND OPERATIONAL RESEARCH": "统计与运筹学",
    "ACCOUNTING AND FINANCE": "会计与金融",
    "ECONOMICS AND ECONOMETRICS": "经济学与计量经济学",
    "MARKETING": "市场营销",
    "DATA SCIENCE AND ARTIFICIAL INTELLIGENCE": "数据科学与人工智能",
    "ENGINEERING AND TECHNOLOGY": "工程与技术",
    "ENGINEERING CHEMICAL": "化学工程",
    "ENGINEERING CIVIL AND STRUCTURAL": "土木与结构工程",
    "ENGINEERING MECHANICAL": "机械工程",
    "ENGINEERING ELECTRICAL AND ELECTRONIC": "电子电气工程",
    "ENGINEERING MINING": "矿物与采矿工程",
    "ENGINEERING PETROLEUM": "石油工程学",
    "ENVIRONMENTAL SCIENCES": "环境科学",
    "EARTH AND MARINE SCIENCES": "地球与海洋科学",
    "BIOLOGICAL SCIENCES": "生物科学",
    "LIFE SCIENCES MEDICINE": "生命科学与医学",
    "ARTS HUMANITIES": "艺术与人文",
    "NATURAL SCIENCES": "自然科学",
    "SOCIAL SCIENCES MANAGEMENT": "社会科学与管理",
    "AGRICULTURE FORESTRY": "农业与林业",
    "ARCHITECTURE BUILT ENVIRONMENT": "建筑与建成环境",
    "HOSPITALITY LEISURE MANAGEMENT": "酒店与休闲管理",
    "SPORTS RELATED SUBJECTS": "体育相关学科",
    "THEOLOGY DIVINITY RELIGIOUS STUDIES": "神学、神学与宗教研究",
    "ANTHROPOLOGY": "人类学",
    "COMMUNICATION AND MEDIA STUDIES": "传播与媒体研究",
    "EDUCATION AND TRAINING": "教育与培训",
    "LIBRARY AND INFORMATION MANAGEMENT": "图书馆与信息管理",
    "PERFORMING ARTS": "表演艺术",
    "ART DESIGN": "艺术与设计",
    "PHILOSOPHY": "哲学",
    "CONDENSED MATTER PHYSICS": "凝聚态物理",
    "ECOLOGY": "生态学",
    "ECONOMICS AND BUSINESS": "经济与商业",
    "ELECTRICAL AND ELECTRONIC ENGINEERING": "电子电气工程",
    "ENDOCRINOLOGY AND METABOLISM": "内分泌与代谢",
    "ENERGY AND FUELS": "能源与燃料",
    "ENVIRONMENT ECOLOGY": "环境/生态学",
    "ENVIRONMENTAL ENGINEERING": "环境工程",
    "GEOSCIENCES": "地球科学",
    "MATERIALS SCIENCE": "材料科学",
    "METEOROLOGY AND ATMOSPHERIC SCIENCES": "气象与大气科学",
    "MICROBIOLOGY": "微生物学",
    "MOLECULAR BIOLOGY AND GENETICS": "分子生物学与遗传学",
    "NANOSCIENCE AND NANOTECHNOLOGY": "纳米科学与纳米技术",
    "OPTICS": "光学",
    "PHARMACOLOGY AND TOXICOLOGY": "药理学与毒理学",
    "PHYSICAL CHEMISTRY": "物理化学",
    "PHYSICS": "物理学",
    "PLANT AND ANIMAL SCIENCE": "植物与动物科学",
    "PSYCHIATRY PSYCHOLOGY": "精神病学/心理学",
    "PUBLIC ENVIRONMENTAL AND OCCUPATIONAL HEALTH": "公共、环境与职业健康",
    "SOCIAL SCIENCES AND PUBLIC HEALTH": "社会科学与公共卫生",
    "SPACE SCIENCE": "空间科学",
    "ARTIFICIAL INTELLIGENCE": "人工智能",
    "BIOLOGY AND BIOCHEMISTRY": "生物学与生物化学",
    "CELL BIOLOGY": "细胞生物学",
    "BIOTECHNOLOGY AND APPLIED MICROBIOLOGY": "生物技术与应用微生物学",
    "AGRICULTURAL SCIENCES": "农业科学",
    "CHEMISTRY": "化学",
    "CIVIL ENGINEERING": "土木工程",
    "COMPUTER SCIENCE": "计算机科学",
    "ECONOMICS AND BUSINESS": "经济与商业",
    "ELECTRICAL AND ELECTRONIC ENGINEERING": "电子电气工程",
    "ENGINEERING": "工程学",
    "GEOSCIENCES": "地球科学",
    "MATERIALS SCIENCE": "材料科学",
    "MATHEMATICS": "数学",
    "MECHANICAL ENGINEERING": "机械工程",
    "NANOSCIENCE AND NANOTECHNOLOGY": "纳米科学与纳米技术",
    "PHYSICS": "物理学",
}


def translate(name_en: str) -> str:
    name_en_upper = name_en.upper().strip()

    # 1. 明确映射优先
    if name_en_upper in EXPLICIT:
        return EXPLICIT[name_en_upper]

    # 2. 处理学科排名：提取 "SUBJECT RANKINGS XXX" 或类似结构
    m = re.search(r"(QS|THE|ARWU|RUR)\s+.*?\b(SUBJECT\s+RANKINGS?)\s+(.+)", name_en_upper)
    if m:
        org, _, subject_part = m.groups()
        subject_key = subject_part.strip()
        subject_cn = SUBJECTS.get(subject_key, subject_key.title())
        org_map = {"QS": "QS", "THE": "泰晤士高等教育", "ARWU": "软科", "RUR": "RUR"}
        return f"{org_map.get(org, org)}{subject_cn}学科排名"

    # 3. USNews 学科排名
    m = re.search(r"USNEWS\s+UNIVERSITY\s+SUBJECT\s+RANKINGS\s+(.+)", name_en_upper)
    if m:
        subject_key = m.group(1).strip()
        subject_cn = SUBJECTS.get(subject_key, subject_key.title())
        return f"U.S. News{subject_cn}学科排名"

    # 4. 通用模式替换
    result = name_en_upper
    for pattern, repl in PATTERNS:
        result = re.sub(pattern, repl, result, flags=re.IGNORECASE)

    # 清理多余空格
    result = re.sub(r"\s+", " ", result).strip()

    # 如果替换后没变（没匹配到任何模式），原样返回但首字母大写
    if result == name_en_upper:
        return name_en.title()

    return result


def main():
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor(dictionary=True)

    cursor.execute("SELECT id, name_zh FROM ranking_source WHERE deleted = 0 ORDER BY id")
    rows = cursor.fetchall()

    updates = []
    for row in rows:
        sid = row["id"]
        current = row["name_zh"]
        cn = translate(current)
        updates.append((cn, current, sid))

    import sys
    dry_run = "--dry-run" in sys.argv

    print(f"Will update {len(updates)} sources")
    for cn, en, sid in updates[:20]:
        print(f"  {sid:3d}: {cn}  ({en})")

    if dry_run:
        print("Dry run, no changes applied.")
        return

    cursor.executemany(
        "UPDATE ranking_source SET name_zh = %s, name_en = %s WHERE id = %s",
        updates,
    )
    conn.commit()
    print(f"Updated {cursor.rowcount} rows.")

    cursor.close()
    conn.close()


if __name__ == "__main__":
    main()
