-- 为院校标签增加简介字段

ALTER TABLE university_tag
    ADD COLUMN description TEXT DEFAULT NULL COMMENT '标签简介';
