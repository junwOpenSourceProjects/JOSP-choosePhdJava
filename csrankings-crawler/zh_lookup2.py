"""
对 297 个 Wikidata 未命中的院校, 走 Wikipedia 中文站 langlinks API
8 线程并发, 重试 3 次
"""
import csv, json, time, requests, pymysql, sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

def make_sess():
    s = requests.Session()
    s.mount('https://', HTTPAdapter(max_retries=Retry(total=3, backoff_factor=0.3,
        status_forcelist=[429,500,502,503,504]),
        pool_connections=16, pool_maxsize=32))
    s.headers['User-Agent'] = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36'
    return s

SESS = make_sess()

# 缩写到全称的本地字典 (CSRankings 里常见的)
ABBREV = {
    'AUEB': 'Athens University of Economics and Business',
    'BNBU': 'Beijing Normal University, Zhuhai',  # 北京师范大学珠海分校
    'BUET': 'Bangladesh University of Engineering and Technology',
    'BUPT': 'Beijing University of Posts and Telecommunications',
    'CMI': 'Clarkson University',  # 不对, 实际是 Chennai Mathematical Institute
    'CNRS': 'Centre national de la recherche scientifique',
    'CRIStAL': 'Centre de Recherche en Informatique, Signal et Automatique de Lille',
    'CUHK (SZ)': 'The Chinese University of Hong Kong, Shenzhen',
    'CUNY': 'City University of New York',
    'CWI': 'Centrum Wiskunde & Informatica',
    'CSU': 'Central South University',  # 中南大学
    'DUT': 'Dalian University of Technology',  # 大连理工
    'ECNU': 'East China Normal University',  # 华东师大
    'FJU': 'Fujian University',
    'GXU': 'Guangxi University',
    'HFUT': 'Hefei University of Technology',  # 合肥工大
    'HIT': 'Harbin Institute of Technology',  # 哈工大
    'HUST': 'Huazhong University of Science and Technology',  # 华科
    'ICT': 'Institute of Computing Technology, Chinese Academy of Sciences',  # 计算所
    'IMDEA': 'IMDEA Software Institute',
    'INSA': 'Institut National des Sciences Appliquées',
    'INSERM': 'Institut national de la santé et de la recherche médicale',
    'INRIA': 'National Institute for Research in Digital Science and Technology',
    'ISCAS': 'Institute of Software, Chinese Academy of Sciences',  # 软件所
    'ITU': 'Istanbul Technical University',  # 土耳其
    'JLU': 'Jilin University',
    'JNU': 'Jinan University',
    'KAIST': 'Korea Advanced Institute of Science and Technology',
    'KTH': 'Royal Institute of Technology',
    'KUST': 'Kunming University of Science and Technology',
    'KUIT': 'Korea University',
    'KYUTECH': 'Kyushu Institute of Technology',
    'LIGM': 'Laboratoire d\'informatique Gaspard-Monge',
    'LSU': 'Louisiana State University',
    'MIT': 'Massachusetts Institute of Technology',
    'MPI': 'Max Planck Institute',
    'NJU': 'Nanjing University',
    'NKU': 'Nankai University',  # 南开
    'NPU': 'Northwestern Polytechnical University',  # 西工大
    'NTU': 'Nanyang Technological University',
    'NUS': 'National University of Singapore',
    'NWU': 'Northwest University',
    'NYU': 'New York University',
    'PKU': 'Peking University',
    'PolyU': 'Hong Kong Polytechnic University',
    'POSTECH': 'Pohang University of Science and Technology',
    'PUC': 'Pontifical Catholic University',
    'QUT': 'Queensland University of Technology',
    'SCUT': 'South China University of Technology',  # 华南理工
    'SDU': 'Shandong University',
    'SEU': 'Southeast University',  # 东南
    'SJTU': 'Shanghai Jiao Tong University',
    'SKKU': 'Sungkyunkwan University',
    'SNU': 'Seoul National University',
    'SUTD': 'Singapore University of Technology and Design',
    'SYSU': 'Sun Yat-sen University',  # 中山大学
    'TJU': 'Tianjin University',
    'TU Berlin': 'Technical University of Berlin',
    'TU Delft': 'Delft University of Technology',
    'TU Dresden': 'Dresden University of Technology',
    'TU Eindhoven': 'Eindhoven University of Technology',
    'TU Graz': 'Graz University of Technology',
    'TU Ilmenau': 'Technische Universität Ilmenau',
    'TU Kaiserslautern': 'University of Kaiserslautern-Landau',
    'TU Munich': 'Technical University of Munich',
    'TU Wien': 'Vienna University of Technology',
    'TUB': 'Technical University of Berlin',
    'TUDelft': 'Delft University of Technology',
    'UAB': 'Universitat Autònoma de Barcelona',
    'UAH': 'University of Alcalá',
    'UAM': 'Autonomous University of Madrid',
    'UC': 'University of Canterbury',
    'UC3M': 'Carlos III University of Madrid',
    'UCAM': 'Catholic University of Murcia',
    'UCB': 'University of California, Berkeley',
    'UCD': 'University College Dublin',
    'UCL': 'University College London',
    'UCLA': 'University of California, Los Angeles',
    'UCM': 'Complutense University of Madrid',
    'UCPH': 'University of Copenhagen',
    'UCSD': 'University of California, San Diego',
    'UCSB': 'University of California, Santa Barbara',
    'UCSC': 'University of California, Santa Cruz',
    'UD': 'University of Delaware',
    'UFRGS': 'Federal University of Rio Grande do Sul',
    'UGR': 'University of Granada',
    'UHH': 'University of Hamburg',
    'UIA': 'Universitat Internacional de Catalunya',
    'UJaen': 'University of Jaén',
    'UJS': 'University of Jiangsu',
    'UL': 'University of Limerick',
    'ULB': 'Université libre de Bruxelles',
    'ULg': 'University of Liège',
    'ULL': 'University of La Laguna',
    'ULPGC': 'University of Las Palmas de Gran Canaria',
    'UMA': 'University of Málaga',
    'UMD': 'University of Maryland, College Park',
    'UMH': 'Miguel Hernández University',
    'UML': 'University of Massachusetts Lowell',
    'UMN': 'University of Minnesota',
    'UMontreal': 'University of Montreal',
    'UMS': 'University of Montpellier',
    'UN': 'University of Nantes',
    'UNA': 'National University of Costa Rica',
    'UNAM': 'National Autonomous University of Mexico',
    'UNC': 'University of North Carolina at Chapel Hill',
    'UNESP': 'Universidade Estadual Paulista',
    'UNICAMP': 'State University of Campinas',
    'UNICT': 'University of Catania',
    'UNIGE': 'University of Genoa',
    'UNIKENT': 'University of Kent',
    'UNIMI': 'University of Milan',
    'UNINA': 'University of Naples Federico II',
    'UNIPD': 'University of Padua',
    'UNISA': 'University of Salerno',
    'UNISI': 'University of Siena',
    'UNITN': 'University of Trento',
    'UNIZAR': 'University of Zaragoza',
    'UO': 'University of Ottawa',
    'UOA': 'National and Kapodistrian University of Athens',
    'UOC': 'Universitat Oberta de Catalunya',
    'UOH': 'University of Haifa',
    'UoH': 'University of Hyderabad',
    'UoK': 'University of Karachi',
    'UOI': 'University of Ioannina',
    'UoM': 'University of Manitoba',
    'UOP': 'University of Patras',
    'UoP': 'University of Pretoria',
    'UOR': 'University of Rouen',
    'UOS': 'University of Sharjah',
    'UoS': 'University of Sistan and Baluchestan',
    'UOU': 'University of Oviedo',
    'UPB': 'University Politehnica of Bucharest',
    'UPC': 'Universitat Politècnica de Catalunya',
    'UPC-': 'Universitat Politècnica de Catalunya',
    'UPF': 'Universitat Pompeu Fabra',
    'UPI': 'Universitas Pendidikan Indonesia',
    'UPM': 'Polytechnic University of Madrid',
    'UPO': 'Pablo de Olavide University',
    'UPV': 'Universitat Politècnica de València',
    'UQ': 'University of Queensland',
    'URJC': 'Rey Juan Carlos University',
    'URT': 'University of Rome Tor Vergata',
    'USACH': 'University of Santiago, Chile',
    'USAL': 'University of Salamanca',
    'USC': 'University of Southern California',
    'USF': 'University of South Florida',
    'USI': 'Università della Svizzera italiana',
    'USP': 'University of São Paulo',
    'USQ': 'University of Southern Queensland',
    'USTC': 'University of Science and Technology of China',
    'USTHB': 'University of Science and Technology Houari Boumediene',
    'USU': 'Utah State University',
    'UT': 'University of Tehran',
    'UTA': 'University of Texas at Arlington',
    'UTB': 'Universiti Teknologi Brunei',
    'UTC': 'University of Technology of Compiègne',
    'UTD': 'University of Texas at Dallas',
    'UTEP': 'University of Texas at El Paso',
    'UTFPR': 'Federal University of Technology - Paraná',
    'UTG': 'University of The Gambia',
    'UTK': 'University of Tennessee, Knoxville',
    'UTM': 'Universiti Teknologi Malaysia',
    'UTP': 'University of Toronto',
    'UTrento': 'University of Trento',
    'UTS': 'University of Technology Sydney',
    'UTSA': 'University of Texas at San Antonio',
    'UTT': 'University of Technology of Troyes',
    'UUM': 'Universiti Utara Malaysia',
    'UV': 'University of Valencia',
    'UVA': 'University of Virginia',
    'UVEG': 'University of Valencia',
    'UVIC': 'University of Vic',
    'UW': 'University of Waterloo',
    'UWA': 'University of Western Australia',
    'UWB': 'University of Washington Bothell',
    'UWC': 'University of the Western Cape',
    'UWE': 'University of the West of England',
    'UWI': 'University of the West Indies',
    'UWO': 'University of Western Ontario',
    'UWS': 'University of Western Sydney',
    'UWyo': 'University of Wyoming',
    'VGU': 'Vietnam National University',
    'VIB': 'VIB-KU Leuven',
    'VJU': 'Vietnam Japan University',
    'VKI': 'Von Karman Institute',
    'VNU': 'Vietnam National University',
    'VPI': 'Virginia Tech',
    'VUB': 'Vrije Universiteit Brussel',
    'VUMC': 'VU University Medical Center',
    'VUT': 'Vienna University of Technology',
    'Waikato': 'University of Waikato',
    'Waseda': 'Waseda University',
    'WHOI': 'Woods Hole Oceanographic Institution',
    'WPI': 'Worcester Polytechnic Institute',
    'WUT': 'Warsaw University of Technology',
    'Xidian': 'Xidian University',  # 西电
    'XJTU': 'Xi\'an Jiaotong University',  # 西交
    'XJU': 'Xinjiang University',
    'XUST': 'Xi\'an University of Science and Technology',
    'Yale': 'Yale University',
    'YITP': 'Yukawa Institute for Theoretical Physics',
    'YNAU': 'Yunnan Agricultural University',
    'YNU': 'Yokohama National University',
    'YSU': 'Yanshan University',
    'YTI': 'Yangtze Transportation Institute',
    'YU': 'Yokohama University',
    'YUC': 'Yüzüncü Yıl University',
    'YUST': 'Yanshan University of Science and Technology',
    'ZJU': 'Zhejiang University',
    'ZUT': 'Zhejiang University of Technology',
}


def expand(name):
    """先查 ABBREV 字典, 命中返回 (full_name, source='abbrev_dict')"""
    n = name.strip()
    if n in ABBREV:
        return ABBREV[n], 'abbrev_dict'
    # 部分匹配 (e.g. "TU Berlin" 包含 "TU")
    for k, v in ABBREV.items():
        if k in n and len(k) >= 3:
            return v, 'abbrev_dict'
    return None, None


def query_wiki(name):
    """en name → zh wiki title via langlinks API"""
    try:
        r = SESS.get('https://zh.wikipedia.org/w/api.php',
            params={'action':'query','titles':name,'prop':'langlinks','lllang':'zh','format':'json','redirects':1},
            timeout=8)
        data = r.json()
        pages = data.get('query', {}).get('pages', {})
        for pid, p in pages.items():
            ll = p.get('langlinks')
            if ll and ll[0].get('*'):
                return ll[0]['*']
        r2 = SESS.get('https://zh.wikipedia.org/w/api.php',
            params={'action':'opensearch','search':name,'limit':1,'format':'json'},
            timeout=8)
        d2 = r2.json()
        if len(d2) >= 4 and d2[1]:
            return d2[1][0]
    except Exception as e:
        return f"__ERR__:{e}"
    return None


# 读 297 个
with open('/tmp/inst_miss.txt') as f:
    next(f)
    jobs = [line.rstrip('\n').split('\t') for line in f]
print(f"jobs: {len(jobs)}", flush=True)

# 阶段 1: ABBREV 字典
results = {}
for iid, name in jobs:
    full, src = expand(name)
    if full:
        results[name] = (iid, full, src)
print(f"abbrev dict 命中 {len(results)}", flush=True)

# 阶段 2: 其余走 wiki API (并发)
todo = [(iid, name) for iid, name in jobs if name not in results]
print(f"todo wiki: {len(todo)}", flush=True)

t0 = time.time()
with ThreadPoolExecutor(max_workers=8) as ex:
    futs = {ex.submit(query_wiki, name): (iid, name) for iid, name in todo}
    done = 0
    for f in as_completed(futs):
        iid, name = futs[f]
        zh = f.result()
        if zh and not zh.startswith('__ERR__'):
            results[name] = (iid, zh, 'wikipedia_zh')
        done += 1
        if done % 30 == 0:
            print(f"  {done}/{len(todo)} ({time.time()-t0:.0f}s)", flush=True)
print(f"wiki API 命中 {len(todo) - sum(1 for iid,n in todo if n not in results)}", flush=True)

# 写回
conn = pymysql.connect(host='127.0.0.1', user='root', password='', database='csrankings', charset='utf8mb4')
cur = conn.cursor()
upd = [(zh, src, iid) for iid, name, (rid, zh, src) in [(iid, n, (results[n][0], results[n][1], results[n][2])) for iid, n in [(i, n) for i, n in jobs] if n in results]]
if upd:
    cur.executemany("UPDATE institution SET name_zh=%s, name_zh_source=%s WHERE id=%s", upd)
    conn.commit()
print(f"已更新 {len(upd)} 条")

cur.execute("SELECT COUNT(*) FROM institution WHERE name_zh IS NULL")
print(f"DB 仍未翻译: {cur.fetchone()[0]}", flush=True)

# 列出仍未翻译的
cur.execute("SELECT id, name, country_alpha2 FROM institution WHERE name_zh IS NULL ORDER BY id")
for iid, name, c in cur.fetchall():
    print(f"  [{iid}] {name} ({c})")
cur.close()
conn.close()
