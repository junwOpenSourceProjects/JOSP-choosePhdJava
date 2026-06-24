#!/usr/bin/env python3
"""为 subject 表补充中文名。当前 name_zh/name_en 都是英文。"""

import mysql.connector

DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "",
    "database": "choosephd",
    "charset": "utf8mb4",
}

SUBJECTS = {
    "life sciences": "生命科学",
    "engineering": "工程学",
    "education": "教育学",
    "law": "法学",
    "business economics": "商业与经济",
    "psychology": "心理学",
    "social sciences": "社会科学",
    "arts humanities": "艺术与人文",
    "physical sciences": "物理科学",
    "clinical health": "临床与健康",
    "computer science": "计算机科学",
    "chemical engineering": "化学工程",
    "automation control": "自动化与控制",
    "chemistry": "化学",
    "biotechnology": "生物技术",
    "aerospace engineering": "航空航天工程",
    "agricultural sciences": "农业科学",
    "biomedical engineering": "生物医学工程",
    "business administration": "工商管理",
    "biological sciences": "生物科学",
    "artificial intelligence": "人工智能",
    "atmospheric science": "大气科学",
    "immunology and microbiology": "免疫学与微生物学",
    "technical sciences": "技术科学",
    "dentistry": "牙医学",
    "pharmacology toxicology and pharmaceutics": "药理学、毒理学与药剂学",
    "economics": "经济学",
    "veterinary": "兽医学",
    "earth and planetary sciences": "地球与行星科学",
    "materials science": "材料科学",
    "neuroscience": "神经科学",
    "biochemistry genetics and molecular biology": "生物化学、遗传学与分子生物学",
    "medicine": "医学",
    "medical sciences": "医学科学",
    "nuclear education and technology": "核教育与技术",
    "life sciences medicine": "生命科学与医学",
    "decision sciences": "决策科学",
    "nanoscience and nanotechnology": "纳米科学与纳米技术",
    "natural sciences": "自然科学",
    "business management and accounting": "商业、管理与会计",
    "business management studies": "商业与管理研究",
    "nursing": "护理学",
    "mathematics": "数学",
    "environmental science": "环境科学",
    "energy": "能源",
    "physics astronomy": "物理与天文学",
    "pharmacy pharmacology": "药学与药理学",
    "modern languages": "现代语言",
    "linguistics": "语言学",
    "law and legal studies": "法律与法律研究",
    "history": "历史",
    "geography": "地理学",
    "geology": "地质学",
    "geophysics": "地球物理学",
    "classics ancient history": "古典文学与古代史",
    "english language and literature": "英语语言与文学",
    "archaeology": "考古学",
    "development studies": "发展研究",
    "sociology": "社会学",
    "politics": "政治学",
    "social policy administration": "社会政策与管理",
    "statistics and operational research": "统计与运筹学",
    "accounting and finance": "会计与金融",
    "economics and econometrics": "经济学与计量经济学",
    "marketing": "市场营销",
    "data science and artificial intelligence": "数据科学与人工智能",
    "engineering and technology": "工程与技术",
    "engineering chemical": "化学工程",
    "engineering civil and structural": "土木与结构工程",
    "engineering mechanical": "机械工程",
    "engineering electrical and electronic": "电子电气工程",
    "engineering mining": "矿物与采矿工程",
    "engineering petroleum": "石油工程学",
    "environmental sciences": "环境科学",
    "earth and marine sciences": "地球与海洋科学",
    "agriculture forestry": "农业与林业",
    "architecture built environment": "建筑与建成环境",
    "hospitality leisure management": "酒店与休闲管理",
    "sports related subjects": "体育相关学科",
    "theology divinity religious studies": "神学、神学与宗教研究",
    "anthropology": "人类学",
    "communication and media studies": "传播与媒体研究",
    "education and training": "教育与培训",
    "library and information management": "图书馆与信息管理",
    "performing arts": "表演艺术",
    "art design": "艺术与设计",
    "philosophy": "哲学",
    "condensed matter physics": "凝聚态物理",
    "ecology": "生态学",
    "economics and business": "经济与商业",
    "electrical and electronic engineering": "电子电气工程",
    "endocrinology and metabolism": "内分泌与代谢",
    "energy and fuels": "能源与燃料",
    "environment ecology": "环境/生态学",
    "environmental engineering": "环境工程",
    "geosciences": "地球科学",
    "meteorology and atmospheric sciences": "气象与大气科学",
    "microbiology": "微生物学",
    "molecular biology and genetics": "分子生物学与遗传学",
    "optics": "光学",
    "pharmacology and toxicology": "药理学与毒理学",
    "physical chemistry": "物理化学",
    "physics": "物理学",
    "plant and animal science": "植物与动物科学",
    "psychiatry psychology": "精神病学/心理学",
    "public environmental and occupational health": "公共、环境与职业健康",
    "social sciences and public health": "社会科学与公共卫生",
    "space science": "空间科学",
    "biology and biochemistry": "生物学与生物化学",
    "cell biology": "细胞生物学",
    "biotechnology and applied microbiology": "生物技术与应用微生物学",
    "civil engineering": "土木工程",
    "mechanical engineering": "机械工程",
    "food science and technology": "食品科学与技术",
    "gastroenterology and hepatology": "胃肠病学与肝病学",
    "green and sustainable science and technology": "绿色与可持续科学与技术",
    "immunology": "免疫学",
    "infectious diseases": "传染病学",
    "marine and freshwater biology": "海洋与淡水生物学",
    "nanoscience and nanotechnology": "纳米科学与纳米技术",
    "neuroscience and behavior": "神经科学与行为",
    "oncology": "肿瘤学",
    "cardiac and cardiovascular systems": "心脏与心血管系统",
    "arts and humanities": "艺术与人文",
    "clinical medicine": "临床医学",
}


def main():
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor(dictionary=True)

    cursor.execute("SELECT id, name_zh FROM subject ORDER BY id")
    rows = cursor.fetchall()

    updates = []
    for row in rows:
        sid = row["id"]
        en = row["name_zh"]
        cn = SUBJECTS.get(en.lower(), en)
        updates.append((cn, en, sid))

    import sys
    dry_run = "--dry-run" in sys.argv
    print(f"Will update {len(updates)} subjects")
    for cn, en, sid in updates[:10]:
        print(f"  {sid:3d}: {cn}  ({en})")

    if dry_run:
        print("Dry run, no changes applied.")
        return

    cursor.executemany(
        "UPDATE subject SET name_zh = %s, name_en = %s WHERE id = %s",
        updates,
    )
    conn.commit()
    print(f"Updated {cursor.rowcount} rows.")

    cursor.close()
    conn.close()


if __name__ == "__main__":
    main()
