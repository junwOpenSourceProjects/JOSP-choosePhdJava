#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
导入全球院校联盟/分层标签到 choosephd 数据库。

说明：
- 标签为新增，不会修改已有的 985/211/双一流/藤校。
- 对重复院校（同名不同 url_id）会选取 url_id 最小的一条做关联，并打印警告。
- 只匹配 deleted=0 的院校。
"""

import pymysql
from contextlib import closing

DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "",
    "database": "choosephd",
    "charset": "utf8mb4",
}

# 每个标签的定义与成员名单
TAGS = [
    {
        "slug": "uk-g5",
        "name_zh": "英国G5",
        "name_en": "G5 Super Elite",
        "category": "foreign",
        "color": "#1E3A8A",
        "sort_order": 110,
        "description": "英国超级精英大学联盟，包括牛津、剑桥、帝国理工、LSE 与 UCL，是英国科研经费与声誉最顶尖的五所大学。",
        "members": [
            "University of Oxford",
            "University of Cambridge",
            "Imperial College London",
            "London School of Economics and Political Science",
            "University College London",
        ],
    },
    {
        "slug": "uk-russell-group",
        "name_zh": "罗素集团",
        "name_en": "Russell Group",
        "category": "foreign",
        "color": "#1E3A8A",
        "sort_order": 210,
        "description": "英国 24 所顶尖研究型大学联盟，成员高校包揽英国约三分之二的研究经费与博士学位授予量。",
        "members": [
            "University of Birmingham",
            "University of Bristol",
            "University of Cambridge",
            "Cardiff University",
            "Durham University",
            "University of Edinburgh",
            "University of Exeter",
            "University of Glasgow",
            "Imperial College London",
            "King's College London",
            "University of Leeds",
            "University of Liverpool",
            "London School of Economics and Political Science",
            "University of Manchester",
            "Newcastle University",
            "University of Nottingham",
            "University of Oxford",
            "Queen Mary University of London",
            "University of Sheffield",
            "University of Southampton",
            "University College London",
            "University of Warwick",
            "University of York",
        ],
    },
    {
        "slug": "au-group-of-eight",
        "name_zh": "澳洲八校联盟",
        "name_en": "Group of Eight",
        "category": "foreign",
        "color": "#CA8A04",
        "sort_order": 220,
        "description": "澳大利亚八所顶尖研究型大学联盟（Go8），成员多为澳洲历史最悠久、科研经费最充足的大型综合大学。",
        "members": [
            "Australian National University",
            "University of Melbourne",
            "University of Sydney",
            "University of Queensland",
            "University of New South Wales",
            "Monash University",
            "University of Western Australia",
            "University of Adelaide",
        ],
    },
    {
        "slug": "jp-sgu-a",
        "name_zh": "日本SGU-A类",
        "name_en": "Japan SGU Type A",
        "category": "foreign",
        "color": "#C2410C",
        "sort_order": 230,
        "description": "日本文部科学省「超级国际化大学计划（SGU）」A 类（Top Type）13 所，目标进入世界大学排名前 100。",
        "members": [
            "Hokkaido University",
            "Tohoku University",
            "University of Tsukuba",
            "University of Tokyo",
            "Tokyo Medical and Dental University",
            "Tokyo Institute of Technology",
            "Nagoya University",
            "Kyoto University",
            "Osaka University",
            "Hiroshima University",
            "Kyushu University",
            "Keio University",
            "Waseda University",
        ],
    },
    {
        "slug": "jp-sgu-b",
        "name_zh": "日本SGU-B类",
        "name_en": "Japan SGU Type B",
        "category": "foreign",
        "color": "#C2410C",
        "sort_order": 310,
        "description": "日本文部科学省「超级国际化大学计划（SGU）」B 类（Global Traction Type）24 所，侧重引领日本社会国际化。",
        "members": [
            "Chiba University",
            "Tokyo University of Foreign Studies",
            "Tokyo University of the Arts",
            "Nagaoka University of Technology",
            "Kanazawa University",
            "Toyohashi University of Technology",
            "Kyoto Institute of Technology",
            "Nara Institute of Science and Technology",
            "Okayama University",
            "Kumamoto University",
            "Akita International University",
            "University of Aizu",
            "International Christian University",
            "Shibaura Institute of Technology",
            "Sophia University",
            "Toyo University",
            "Hosei University",
            "Meiji University",
            "Rikkyo University",
            "Soka University",
            "International University of Japan",
            "Ritsumeikan University",
            "Kwansei Gakuin University",
            "Ritsumeikan Asia Pacific University",
        ],
    },
    {
        "slug": "jp-soukai-jouri",
        "name_zh": "早庆上理",
        "name_en": "Sōkei-Jōri",
        "category": "foreign",
        "color": "#C2410C",
        "sort_order": 130,
        "description": "日本首都圈私立大学最顶尖梯队，由早稻田大学、庆应义塾大学、上智大学、东京理科大学四所组成。",
        "members": [
            "Waseda University",
            "Keio University",
            "Sophia University",
            "Tokyo University of Science",
        ],
    },
    {
        "slug": "jp-march",
        "name_zh": "MARCH",
        "name_en": "MARCH Universities",
        "category": "foreign",
        "color": "#C2410C",
        "sort_order": 320,
        "description": "日本关东地区五所著名私立大学（明治、青山学院、立教、中央、法政），是日本首都圈私立名校的核心层。",
        "members": [
            "Meiji University",
            "Aoyama Gakuin University",
            "Rikkyo University",
            "Chuo University",
            "Hosei University",
        ],
    },
    {
        "slug": "jp-kankankandouri",
        "name_zh": "关关同立",
        "name_en": "Kankankandōri",
        "category": "foreign",
        "color": "#C2410C",
        "sort_order": 330,
        "description": "日本关西地区四大著名私立大学（关西大学、关西学院大学、同志社大学、立命馆大学），与关东 MARCH 齐名。",
        "members": [
            "Kansai University",
            "Kwansei Gakuin University",
            "Doshisha University",
            "Ritsumeikan University",
        ],
    },
    {
        "slug": "kr-sky",
        "name_zh": "韩国SKY",
        "name_en": "SKY Universities",
        "category": "foreign",
        "color": "#15803D",
        "sort_order": 120,
        "description": "韩国社会公认最顶尖的三所大学：首尔大学（Seoul National University）、高丽大学（Korea University）、延世大学（Yonsei University）。",
        "members": [
            "Seoul National University",
            "Korea University",
            "Yonsei University",
        ],
    },
    {
        "slug": "de-university-of-excellence",
        "name_zh": "德国精英大学",
        "name_en": "German Universities of Excellence",
        "category": "foreign",
        "color": "#B45309",
        "sort_order": 240,
        "description": "德国联邦与州政府评选的「精英大学」及「精英联盟」，代表德国国际竞争力最强的研究型大学。",
        "members": [
            "RWTH Aachen University",
            "Free University of Berlin",
            "Humboldt University of Berlin",
            "Technical University of Berlin",
            "University of Bonn",
            "Dresden University of Technology",
            "University of Hamburg",
            "Heidelberg University",
            "Karlsruhe Institute of Technology",
            "University of Konstanz",
            "Ludwig Maximilian University of Munich",
            "Technical University of Munich",
            "University of Tübingen",
        ],
    },
    {
        "slug": "fr-idex",
        "name_zh": "法国IDEX",
        "name_en": "France IDEX",
        "category": "foreign",
        "color": "#4338CA",
        "sort_order": 250,
        "description": "法国「卓越计划」（Initiatives d'Excellence）支持的顶尖大学与大学共同体，是法国建设世界一流高校的核心项目。",
        "members": [
            "Aix-Marseille University",
            "University of Bordeaux",
            "Grenoble Alpes University",
            "Claude Bernard Lyon 1 University",
            "University of Montpellier",
            "Université Côte d'Azur",
            "Paris Sciences et Lettres University",
            "Paris-Saclay University",
            "Sorbonne University",
            "University of Strasbourg",
            "Toulouse III - Paul Sabatier University",
        ],
    },
    {
        "slug": "fr-grandes-ecoles",
        "name_zh": "法国顶尖高商/工程师学院",
        "name_en": "Grandes Écoles",
        "category": "foreign",
        "color": "#4338CA",
        "sort_order": 340,
        "description": "法国精英教育体系中的顶尖高等专业学院（Grandes Écoles），包括综合理工、高商、政治学院、工程师学院等，录取高度 selective。",
        "members": [
            "École Polytechnique",
            "HEC Paris",
            "Sciences Po",
            "École Normale Supérieure",
            "Mines Paris - PSL",
            "École des Ponts ParisTech",
            "CentraleSupélec",
            "ESSEC Business School",
            "EDHEC Business School",
            "INSEAD",
        ],
    },
    {
        "slug": "it-c9",
        "name_zh": "意大利C9",
        "name_en": "Italy Top 9",
        "category": "foreign",
        "color": "#15803D",
        "sort_order": 350,
        "description": "留学圈对中国 C9 的类比说法，指意大利最负盛名、国际排名最高的 9 所综合性与研究型大学（非意大利官方联盟）。",
        "members": [
            "University of Bologna",
            "Sapienza University of Rome",
            "Politecnico di Milano",
            "University of Milan",
            "University of Padua",
            "University of Turin",
            "University of Florence",
            "University of Pisa",
            "University of Naples Federico II",
        ],
    },
    {
        "slug": "ca-u15",
        "name_zh": "加拿大U15",
        "name_en": "U15 Group of Canadian Research Universities",
        "category": "foreign",
        "color": "#9D174D",
        "sort_order": 260,
        "description": "加拿大 15 所顶尖研究型大学联盟，成员承担加拿大约 80% 的科研专利与博士学位授予。",
        "members": [
            "University of Alberta",
            "University of British Columbia",
            "University of Calgary",
            "Dalhousie University",
            "Université Laval",
            "University of Manitoba",
            "McGill University",
            "McMaster University",
            "Université de Montréal",
            "University of Ottawa",
            "Queen's University",
            "University of Saskatchewan",
            "University of Toronto",
            "University of Waterloo",
            "Western University",
        ],
    },
    {
        "slug": "us-public-ivies",
        "name_zh": "美国公立常青藤",
        "name_en": "Public Ivies",
        "category": "foreign",
        "color": "#BE123C",
        "sort_order": 270,
        "description": "1985 年 Richard Moll 提出的概念，指提供媲美藤校教育体验的公立大学，最初名单含 15 所（含 8 所 UC 分校）。",
        "members": [
            "College of William & Mary",
            "Miami University",
            "University of California, Berkeley",
            "University of California, Davis",
            "University of California, Irvine",
            "University of California, Los Angeles",
            "University of California, Riverside",
            "University of California, San Diego",
            "University of California, Santa Barbara",
            "University of California, Santa Cruz",
            "University of Michigan",
            "University of North Carolina at Chapel Hill",
            "University of Texas at Austin",
            "University of Vermont",
            "University of Virginia",
        ],
    },
    {
        "slug": "us-new-ivies",
        "name_zh": "美国新常青藤",
        "name_en": "New Ivies",
        "category": "foreign",
        "color": "#0F766E",
        "sort_order": 280,
        "description": "Forbes 2026 年「新藤校」名单，共 20 所（10 公立 + 10 私立），代表美国就业市场与雇主认可度最高的非传统藤校。",
        "members": [
            "University of California, Berkeley",
            "University of Michigan",
            "University of North Carolina at Chapel Hill",
            "University of Virginia",
            "Georgia Institute of Technology",
            "University of Illinois Urbana-Champaign",
            "University of Texas at Austin",
            "University of Wisconsin-Madison",
            "University of Maryland, College Park",
            "University of Washington",
            "Johns Hopkins University",
            "Northwestern University",
            "University of Notre Dame",
            "Rice University",
            "Vanderbilt University",
            "Emory University",
            "Carnegie Mellon University",
            "Georgetown University",
            "Tufts University",
            "University of Southern California",
        ],
    },
    {
        "slug": "us-big-ten",
        "name_zh": "美国Big Ten",
        "name_en": "Big Ten Academic Alliance",
        "category": "foreign",
        "color": "#0369A1",
        "sort_order": 290,
        "description": "美国中西部/东西海岸 18 所大型公立研究型大学体育与学术联盟（2024 年扩员后含 UCLA、USC、Oregon、Washington）。",
        "members": [
            "University of Illinois Urbana-Champaign",
            "Indiana University Bloomington",
            "University of Iowa",
            "University of Maryland",
            "University of Michigan",
            "Michigan State University",
            "University of Minnesota",
            "University of Nebraska-Lincoln",
            "Northwestern University",
            "Ohio State University",
            "Pennsylvania State University",
            "Purdue University",
            "Rutgers University",
            "University of Wisconsin-Madison",
            "University of Oregon",
            "University of Washington",
            "University of California, Los Angeles",
            "University of Southern California",
        ],
    },
    {
        "slug": "ru-project-5-100",
        "name_zh": "俄罗斯5-100计划",
        "name_en": "Project 5-100",
        "category": "foreign",
        "color": "#6B21A8",
        "sort_order": 295,
        "description": "俄罗斯国家学术卓越计划，遴选 21 所重点大学，目标使至少 5 所俄罗斯高校进入世界大学排名前 100。",
        "members": [
            "Immanuel Kant Baltic Federal University",
            "Higher School of Economics",
            "Far Eastern Federal University",
            "Kazan Federal University",
            "Moscow Institute of Physics and Technology",
            "National University of Science and Technology",
            "National Research Nuclear University",
            "Lobachevsky University",
            "Novosibirsk State University",
            "First Moscow State Medical University",
            "Peoples' Friendship University of Russia",
            "Samara National Research University",
            "Saint-Petersburg Electrotechnical University",
            "Peter the Great St. Petersburg Polytechnic University",
            "Siberian Federal University",
            "Tomsk State University",
            "Tomsk Polytechnic University",
            "University of Tyumen",
            "ITMO University",
            "South Ural State University",
            "Ural Federal University",
        ],
    },
]


# 常见名称变体映射：键为脚本中使用的标准名，值为数据库中可能存在的别名
NAME_ALIASES = {
    # UK
    "London School of Economics and Political Science": [
        "London School of Economics",
        "LSE",
    ],
    "University College London": ["UCL"],
    "King's College London": ["Kings College London"],
    # Australia
    "University of New South Wales": ["UNSW"],
    # Japan
    "Tokyo Medical and Dental University": ["Tokyo Medical & Dental University"],
    "Tokyo University of the Arts": ["Tokyo National University of the Arts"],
    "Tokyo University of Foreign Studies": ["Tokyo University of Foreign Studies"],
    "Nara Institute of Science and Technology": ["NAIST"],
    "Ritsumeikan Asia Pacific University": ["Ritsumeikan Asia Pacific University"],
    # Germany
    "Free University of Berlin": ["Free University Berlin", "Freie Universität Berlin"],
    "Humboldt University of Berlin": ["Humboldt-Universität zu Berlin"],
    "Technical University of Berlin": ["Technische Universität Berlin"],
    "Dresden University of Technology": ["TU Dresden", "Technische Universität Dresden"],
    "Karlsruhe Institute of Technology": ["KIT"],
    "Ludwig Maximilian University of Munich": ["LMU Munich", "Ludwig-Maximilians-Universität München"],
    "Technical University of Munich": ["TUM", "Technische Universität München"],
    "University of Tübingen": ["Eberhard Karls University of Tübingen", "University of Tuebingen"],
    # France
    "Aix-Marseille University": ["Aix-Marseille Université"],
    "University of Bordeaux": ["Université de Bordeaux"],
    "Grenoble Alpes University": ["Université Grenoble Alpes"],
    "Claude Bernard Lyon 1 University": ["Université Claude Bernard Lyon 1", "University of Lyon", "Claude Bernard University Lyon 1"],
    "University of Montpellier": ["Université de Montpellier"],
    "Université Côte d'Azur": ["University of Nice", "University of Nice Sophia Antipolis"],
    "Paris Sciences et Lettres University": ["PSL University", "PSL Research University", "Université Psl"],
    "Paris-Saclay University": ["Université Paris-Saclay", "Universite Paris Saclay (university of Paris-sud, Paris 11)"],
    "Sorbonne University": ["Sorbonne Université"],
    "University of Strasbourg": ["Université de Strasbourg"],
    "Toulouse III - Paul Sabatier University": ["Université Toulouse III - Paul Sabatier", "Paul Sabatier University, Toulouse 3"],
    "École Polytechnique": ["Ecole Polytechnique"],
    "HEC Paris": ["HEC"],
    "Sciences Po": ["Institut d'Etudes Politiques de Paris"],
    "École Normale Supérieure": ["Ecole Normale Supérieure", "ENS"],
    "Mines Paris - PSL": ["Mines ParisTech"],
    "École des Ponts ParisTech": ["Ecole des Ponts ParisTech"],
    "CentraleSupélec": ["CentraleSupelec"],
    "ESSEC Business School": ["ESSEC"],
    "EDHEC Business School": ["EDHEC"],
    "INSEAD": ["INSEAD"],
    # Italy
    "Sapienza University of Rome": ["Sapienza - University of Rome", "Sapienza University"],
    "Politecnico di Milano": ["Polytechnic University of Milan", "Polytechnic Institute of Milan"],
    "University of Naples Federico II": ["University of Naples"],
    # Canada
    "Université Laval": ["Laval University"],
    "Université de Montréal": ["University of Montreal"],
    "Western University": ["University of Western Ontario"],
    # US
    "College of William & Mary": ["William & Mary"],
    "Miami University": ["Miami University, Oxford"],
    "University of California, Berkeley": ["University of California Berkeley"],
    "University of California, Davis": ["University of California Davis"],
    "University of California, Irvine": ["University of California Irvine"],
    "University of California, Los Angeles": ["UCLA", "University of California Los Angeles"],
    "University of California, Riverside": ["University of California Riverside"],
    "University of California, San Diego": ["University of California San Diego"],
    "University of California, Santa Barbara": ["University of California Santa Barbara"],
    "University of California, Santa Cruz": ["University of California Santa Cruz"],
    "University of North Carolina at Chapel Hill": ["University of North Carolina, Chapel Hill"],
    "University of Texas at Austin": ["University of Texas, Austin"],
    "University of Virginia": ["UVA"],
    "Georgia Institute of Technology": ["Georgia Tech"],
    "University of Illinois Urbana-Champaign": ["University of Illinois at Urbana-Champaign"],
    "University of Wisconsin-Madison": ["University of Wisconsin, Madison"],
    "University of Maryland, College Park": ["University of Maryland"],
    "Johns Hopkins University": ["Johns Hopkins"],
    "Northwestern University": ["Northwestern"],
    "University of Notre Dame": ["Notre Dame"],
    "Rice University": ["Rice"],
    "Vanderbilt University": ["Vanderbilt"],
    "Emory University": ["Emory"],
    "Carnegie Mellon University": ["CMU", "Carnegie Mellon"],
    "Georgetown University": ["Georgetown"],
    "Tufts University": ["Tufts"],
    "University of Southern California": ["USC"],
    "Indiana University Bloomington": ["Indiana University"],
    "University of Iowa": ["University of Iowa"],
    "University of Maryland": ["University of Maryland, College Park"],
    "Michigan State University": ["MSU"],
    "University of Minnesota": ["University of Minnesota, Twin Cities"],
    "University of Nebraska-Lincoln": ["University of Nebraska Lincoln"],
    "Ohio State University": ["The Ohio State University"],
    "Pennsylvania State University": ["Penn State University", "Penn State"],
    "Purdue University": ["Purdue"],
    "Rutgers University": ["Rutgers, The State University of New Jersey"],
    "University of Oregon": ["University of Oregon"],
    # Russia
    "Higher School of Economics": ["HSE University", "National Research University Higher School of Economics"],
    "Far Eastern Federal University": ["Far Eastern Federal University"],
    "Kazan Federal University": ["Kazan, Volga Region Federal University"],
    "Moscow Institute of Physics and Technology": ["MIPT"],
    "National University of Science and Technology": ["MISIS", "NUST MISIS"],
    "National Research Nuclear University": ["MEPhI", "NRNU MEPhI"],
    "Lobachevsky University": ["Lobachevsky State University of Nizhny Novgorod", "UNN"],
    "Novosibirsk State University": ["Novosibirsk State University"],
    "First Moscow State Medical University": ["Sechenov University", "I.M. Sechenov First Moscow State Medical University"],
    "Peoples' Friendship University of Russia": ["RUDN University"],
    "Samara National Research University": ["Samara University"],
    "Saint-Petersburg Electrotechnical University": ["LETI", "St. Petersburg Electrotechnical University", "St Petersburg Electrotechnical University"],
    "Peter the Great St. Petersburg Polytechnic University": ["SPbPU", "Peter The Great Saint-petersburg Polytechnic University"],
    "Siberian Federal University": ["SibFU"],
    "Tomsk State University": ["TSU"],
    "Tomsk Polytechnic University": ["TPU"],
    "University of Tyumen": ["Tyumen State University"],
    "ITMO University": ["ITMO"],
    "South Ural State University": ["SUSU"],
    "Ural Federal University": ["UrFU"],
}


def normalize(s: str) -> str:
    return " ".join(s.lower().replace(",", " ").replace("-", " ").split())


def build_name_index(cursor):
    """构建院校名称到 url_id 的索引，返回 {标准名: [url_id,...]} 及匹配集合。"""
    cursor.execute(
        "SELECT url_id, name_zh, name_en FROM university WHERE deleted=0"
    )
    rows = cursor.fetchall()

    # 主索引：标准名 -> url_id 列表
    primary = {}
    # 辅助索引：各种 cleaned 名称 -> url_id
    lookup = {}

    for url_id, name_zh, name_en in rows:
        names = set()
        if name_zh:
            names.add(name_zh.strip())
            names.add(name_zh.strip().replace("，", ",").replace("、", ","))
        if name_en:
            names.add(name_en.strip())
        for n in names:
            if n:
                primary.setdefault(n, []).append(url_id)
            lookup.setdefault(normalize(n), []).append(url_id)

    return primary, lookup


def find_university(name, primary, lookup):
    """尝试精确匹配，再尝试别名与模糊匹配。返回单个 url_id 或 None。"""
    # 1. 精确主名
    if name in primary:
        return sorted(primary[name])[0]

    # 2. 别名精确
    for alias in NAME_ALIASES.get(name, []):
        if alias in primary:
            return sorted(primary[alias])[0]

    # 3. 归一化匹配（允许顺序差异 / 大小写 / 标点）
    norm = normalize(name)
    if norm in lookup:
        return sorted(lookup[norm])[0]

    for alias in NAME_ALIASES.get(name, []):
        norm_alias = normalize(alias)
        if norm_alias in lookup:
            return sorted(lookup[norm_alias])[0]

    # 4. 子串匹配：主名作为子串出现在数据库英文名中
    for db_name, ids in primary.items():
        if db_name and (name.lower() in db_name.lower() or db_name.lower() in name.lower()):
            return sorted(ids)[0]

    return None


def main():
    with closing(pymysql.connect(**DB_CONFIG)) as conn:
        with conn.cursor() as cursor:
            primary, lookup = build_name_index(cursor)

            for tag in TAGS:
                # 插入或更新标签
                cursor.execute(
                    """
                    INSERT INTO university_tag
                        (slug, name_zh, name_en, category, color, sort_order, description, active, deleted)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, 1, 0)
                    ON DUPLICATE KEY UPDATE
                        name_zh = VALUES(name_zh),
                        name_en = VALUES(name_en),
                        category = VALUES(category),
                        color = VALUES(color),
                        sort_order = VALUES(sort_order),
                        description = VALUES(description),
                        active = 1,
                        deleted = 0
                    """,
                    (
                        tag["slug"],
                        tag["name_zh"],
                        tag["name_en"],
                        tag["category"],
                        tag["color"],
                        tag["sort_order"],
                        tag["description"],
                    ),
                )
                cursor.execute("SELECT id FROM university_tag WHERE slug=%s", (tag["slug"],))
                tag_id = cursor.fetchone()[0]

                matched = 0
                newly_inserted = 0
                unmatched = []
                for member in tag["members"]:
                    url_id = find_university(member, primary, lookup)
                    if url_id is None:
                        unmatched.append(member)
                        continue

                    matched += 1

                    # 避免重复关联
                    cursor.execute(
                        """
                        SELECT 1 FROM university_tag_relation
                        WHERE university_id=%s AND tag_id=%s
                        """,
                        (url_id, tag_id),
                    )
                    if cursor.fetchone():
                        continue

                    cursor.execute(
                        """
                        INSERT INTO university_tag_relation (university_id, tag_id)
                        VALUES (%s, %s)
                        """,
                        (url_id, tag_id),
                    )
                    newly_inserted += 1

                print(
                    f"[{tag['slug']}] {tag['name_zh']}: 库中已匹配 {matched}/{len(tag['members'])} 所，本次新增 {newly_inserted} 条关联"
                )
                if unmatched:
                    for m in unmatched:
                        print(f"  - 未匹配: {m}")

            conn.commit()


if __name__ == "__main__":
    main()
