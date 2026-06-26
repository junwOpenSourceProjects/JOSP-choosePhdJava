# 83 院校中文名 (手工补, 来源 Wikipedia + 国内惯用译名)
# 注意: NULL 表示保留英文; 否则填中文
ABBREV_FINAL = {
    72:  ('伦敦城市大学', 'wikipedia_zh'),
    174: ('马德里 IMDEA 网络研究所', 'wikipedia_zh'),
    175: ('马德里 IMDEA 软件研究所', 'wikipedia_zh'),
    283: ('印度国立技术学院 西尔卡', 'wikipedia_zh'),
    284: ('印度国立技术学院 瓦朗加尔', 'wikipedia_zh'),
    291: ('纽约理工大学', 'wikipedia_zh'),
    297: ('东北大学(中国)', 'manual'),
    318: ('巴布纳科学技术大学', 'manual'),
    319: ('帕德博恩大学', 'wikipedia_zh'),
    322: ('普拉克沙大学', 'wikipedia_zh'),
    325: ('加泰罗尼亚理工大学', 'wikipedia_zh'),
    326: ('蒙特利尔理工学院', 'wikipedia_zh'),
    330: ('卡塔尔计算研究所', 'manual'),
    334: ('昆士兰科技大学', 'wikipedia_zh'),
    339: ('拉杰沙希工程技术大学', 'manual'),
    340: ('雷文肖大学', 'wikipedia_zh'),
    345: ('伦敦大学皇家霍洛威学院', 'wikipedia_zh'),
    349: ('南伊利诺伊大学 卡本代尔分校', 'wikipedia_zh'),
    350: ('SRM 科技研究所', 'manual'),
    359: ('圣安娜高等学校', 'wikipedia_zh'),
    367: ('希夫·纳达尔大学', 'wikipedia_zh'),
    374: ('华南理工大学', 'wikipedia_zh'),
    387: ('托卡特经济与科技大学', 'manual'),
    391: ('开姆尼茨工业大学', 'wikipedia_zh'),
    392: ('克劳斯塔尔工业大学', 'wikipedia_zh'),
    394: ('达姆施塔特工业大学', 'wikipedia_zh'),
    396: ('多特蒙德工业大学', 'wikipedia_zh'),
    398: ('埃因霍温理工大学', 'wikipedia_zh'),
    399: ('弗莱贝格工业大学', 'wikipedia_zh'),
    400: ('伊尔默瑙工业大学', 'wikipedia_zh'),
    401: ('凯泽斯劳滕工业大学', 'wikipedia_zh'),
    402: ('慕尼黑工业大学', 'wikipedia_zh'),
    407: ('塔塔基础研究所', 'wikipedia_zh'),
    413: ('卡塔尔德州农工大学', 'manual'),
    416: ('韩国天主教大学', 'wikipedia_zh'),
    417: ('香港理工大学', 'wikipedia_zh'),
    439: ('南里奥格兰德联邦大学', 'wikipedia_zh'),
    442: ('北卡罗来纳大学夏洛特分校', 'wikipedia_zh'),
    443: ('北卡罗来纳大学格林斯伯勒分校', 'wikipedia_zh'),
    448: ('圣保罗大学 圣卡洛斯分校', 'manual'),
    450: ('挪威北极圈大学', 'wikipedia_zh'),
    451: ('阿肯色大学小石城分校', 'wikipedia_zh'),
    452: ('加州大学伯克利分校', 'wikipedia_zh'),
    453: ('加州大学戴维斯分校', 'wikipedia_zh'),
    454: ('加州大学欧文分校', 'wikipedia_zh'),
    455: ('加州大学洛杉矶分校', 'wikipedia_zh'),
    456: ('加州大学默塞德分校', 'wikipedia_zh'),
    457: ('加州大学河滨分校', 'wikipedia_zh'),
    458: ('加州大学圣地亚哥分校', 'wikipedia_zh'),
    459: ('加州大学圣巴巴拉分校', 'wikipedia_zh'),
    460: ('加州大学圣克鲁兹分校', 'wikipedia_zh'),
    461: ('伊利诺伊大学厄巴纳-香槟分校', 'wikipedia_zh'),
    462: ('路易斯安那大学拉法叶分校', 'wikipedia_zh'),
    463: ('马里兰大学巴尔的摩县分校', 'wikipedia_zh'),
    466: ('密苏里大学堪萨斯城分校', 'wikipedia_zh'),
    467: ('摩德纳和雷焦艾米利亚大学', 'wikipedia_zh'),
    468: ('威斯康星大学密尔沃基分校', 'wikipedia_zh'),
    469: ('科尔多瓦国立大学', 'wikipedia_zh'),
    474: ('维索萨联邦大学', 'wikipedia_zh'),
    475: ('新里斯本大学', 'wikipedia_zh'),
    476: ('巴西利亚大学', 'wikipedia_zh'),
    477: ('里斯本大学', 'wikipedia_zh'),
    480: ('奥尔巴尼大学', 'wikipedia_zh'),
    485: ('阿拉巴马大学亨茨维尔分校', 'wikipedia_zh'),
    540: ('圭尔夫大学', 'wikipedia_zh'),
    543: ('哈勒-维滕贝格大学', 'wikipedia_zh'),
    546: ('夏威夷大学马诺阿分校', 'wikipedia_zh'),
    602: ('内华达大学', 'wikipedia_zh'),
    603: ('内华达大学拉斯维加斯分校', 'wikipedia_zh'),
    609: ('尼科西亚大学', 'wikipedia_zh'),
    617: ('奥斯纳布吕克大学', 'wikipedia_zh'),
    626: ('比雷埃夫斯大学', 'wikipedia_zh'),
    640: ('桑尼奥大学', 'wikipedia_zh'),
    659: ('德克萨斯大学埃尔帕索分校', 'wikipedia_zh'),
    697: ('让·莫内大学', 'wikipedia_zh'),
    699: ('巴黎西岱大学', 'wikipedia_zh'),
    700: ('巴黎第九大学', 'wikipedia_zh'),
    702: ('谢布鲁克大学', 'wikipedia_zh'),
    703: ('魁北克大学蒙特利尔分校', 'wikipedia_zh'),
    709: ('印度国立技术学院 那格浦尔', 'manual'),
    710: ('河内国家大学 工程技术大学', 'manual'),
    711: ('阿姆斯特丹自由大学', 'wikipedia_zh'),
    714: ('Vin大学', 'manual'),
}

import pymysql
conn = pymysql.connect(host='127.0.0.1', user='root', password='', database='csrankings', charset='utf8mb4')
cur = conn.cursor()
upd = [(zh, src, iid) for iid, (zh, src) in ABBREV_FINAL.items()]
cur.executemany("UPDATE institution SET name_zh=%s, name_zh_source=%s WHERE id=%s", upd)
conn.commit()
print(f"更新 {cur.rowcount} 条")

cur.execute("SELECT name_zh_source, COUNT(*) FROM institution GROUP BY name_zh_source")
for src, n in cur.fetchall():
    print(f"  {src or 'NULL'}: {n}")

cur.execute("SELECT COUNT(*) FROM institution WHERE name_zh IS NULL")
left = cur.fetchone()[0]
print(f"剩余 NULL: {left}")
if left > 0:
    cur.execute("SELECT id, name FROM institution WHERE name_zh IS NULL ORDER BY id")
    for r in cur.fetchall():
        print(f"  [{r[0]}] {r[1]}")
cur.close()
conn.close()