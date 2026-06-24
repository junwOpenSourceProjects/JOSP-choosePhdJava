-- Region dictionary
INSERT INTO region (id, slug, name_zh, name_en) VALUES
(1, 'asia', '亚洲', 'Asia'),
(2, 'europe', '欧洲', 'Europe'),
(3, 'north-america', '北美洲', 'North America'),
(4, 'south-america', '南美洲', 'South America'),
(5, 'africa', '非洲', 'Africa'),
(6, 'oceania', '大洋洲', 'Oceania'),
(7, 'antarctica', '南极洲', 'Antarctica')
ON DUPLICATE KEY UPDATE name_zh = VALUES(name_zh), name_en = VALUES(name_en);
