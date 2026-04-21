# 🎓 JOSP-ChoosePhdJava - 大学排名查询系统后端

![Java](https://img.shields.io/badge/Java-25-orange?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen?logo=springboot)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.16-red?logo=mybatis)
![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue?logo=mysql)
![License](https://img.shields.io/badge/License-AGPL--3.0-blue)

## 📖 项目简介

JOSP-ChoosePhdJava 是一个大学排名查询系统的后端服务，提供 QS、US News 等世界大学排名数据的查询、筛选和可视化功能。支持按国家、大洲、排名等条件查询大学信息，为考研/留学择校提供数据支持。

**关联前端项目**: [JOSP-choosePhdVue3](../JOSP-choosePhdVue3)

## 🏗️ 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 25 |
| 框架 | Spring Boot | 3.4.4 |
| ORM | MyBatis-Plus | 3.5.16 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | 6.0+ |
| API文档 | Knife4j | 3.0.3 |
| JSON处理 | fastjson2 | 2.0.61 |
| 工具库 | Hutool | 5.8.44 |
| 分页插件 | PageHelper | 6.1.0 |
| 多数据源 | dynamic-datasource | 4.3.1 |

## 🚀 快速开始

### 环境要求

- JDK 25+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 安装步骤

```bash
# 1. 克隆项目
git clone https://github.com/yourusername/JOSP-choosePhdJava.git

# 2. 进入项目目录
cd JOSP-choosePhdJava

# 3. 配置数据库
# 修改 src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_database?useSSL=false&serverTimezone=UTC
    username: your_username
    password: your_password

# 4. 安装依赖
mvn clean install

# 5. 启动项目
mvn spring-boot:run
```

### 接口文档

启动项目后访问 Knife4j 文档地址：`http://localhost:8080/doc.html`

## 🗄️ 数据库表结构

### 1. login_user - 用户登录表
```sql
CREATE TABLE login_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(50) COMMENT '用户名',
    password VARCHAR(100) COMMENT '密码(MD5加密)',
    create_time DATETIME COMMENT '创建时间'
) COMMENT='用户登录表';
```

### 2. university_rankings_all - 大学排名汇总表
```sql
CREATE TABLE university_rankings_all (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(200) COMMENT '大学名称(中文)',
    university_name_english VARCHAR(200) COMMENT '大学名称(英文)',
    university_tags VARCHAR(100) COMMENT '大学标签(国家)',
    university_tags_state VARCHAR(100) COMMENT '大学标签(大洲)',
    ranking_year INT COMMENT '排名年份',
    current_rank_integer_qs INT COMMENT 'QS当前排名',
    current_rank_integer_qs_cs INT COMMENT 'QS计算机排名',
    current_rank_integer_usnews INT COMMENT 'US News当前排名',
    current_rank_integer_usnews_cs INT COMMENT 'US News计算机排名'
) COMMENT='大学排名数据汇总表';
```

### 3. choose_phd - 院校信息表
```sql
CREATE TABLE choose_phd (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    university_name VARCHAR(200) COMMENT '大学名称',
    ranking_data TEXT COMMENT '大学排名相关数据',
    official_website VARCHAR(500) COMMENT '院校官网链接',
    recruitment_website VARCHAR(500) COMMENT '社招网站链接',
    priority INT COMMENT '优先级',
    country_region VARCHAR(100) COMMENT '国家/地区',
    scholarship TEXT COMMENT '奖学金信息',
    salary_amount DECIMAL(10,2) COMMENT '薪资金额',
    salary_currency VARCHAR(20) COMMENT '薪资货币类型',
    living_expenses_amount DECIMAL(10,2) COMMENT '生活费用',
    living_expenses_currency VARCHAR(20) COMMENT '生活费用货币类型',
    research_field TEXT COMMENT '研究方向',
    application_requirements TEXT COMMENT '申请要求',
    application_deadline DATE COMMENT '招生截止时间',
    drug_prohibition BOOLEAN COMMENT '是否禁毒',
    gun_control BOOLEAN COMMENT '是否控枪',
    qs_rank INT COMMENT 'QS排名',
    usnews_rank INT COMMENT 'US News排名',
    education_duration VARCHAR(50) COMMENT '学制',
    application_difficulty VARCHAR(50) COMMENT '申请难度',
    reference_material TEXT COMMENT '参考资料'
) COMMENT='院校详细信息表';
```

### 4. university_rankings_qs - QS排名表
```sql
CREATE TABLE university_rankings_qs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(200) COMMENT '大学名称(中文)',
    university_name_english VARCHAR(200) COMMENT '大学名称(英文)',
    university_tags VARCHAR(100) COMMENT '国家',
    university_tags_state VARCHAR(100) COMMENT '大洲',
    ranking_year INT COMMENT '排名年份',
    current_rank_integer INT COMMENT '当前排名'
) COMMENT='QS世界大学排名表';
```

### 5. university_rankings_usnews - US News排名表
```sql
CREATE TABLE university_rankings_usnews (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    university_name_chinese VARCHAR(200) COMMENT '大学名称(中文)',
    university_name_english VARCHAR(200) COMMENT '大学名称(英文)',
    university_tags VARCHAR(100) COMMENT '国家',
    university_tags_state VARCHAR(100) COMMENT '大洲',
    ranking_year INT COMMENT '排名年份',
    current_rank_integer INT COMMENT '当前排名'
) COMMENT='US News世界大学排名表';
```

## 📡 主要 API 接口

### 用户认证

| 接口 | 方法 | 描述 |
|------|------|------|
| `/vue-element-admin/user/login` | POST | 用户登录（MD5密码验证） |
| `/vue-element-admin/user/info` | GET | 获取用户角色信息 |

### 大学排名查询

| 接口 | 方法 | 描述 |
|------|------|------|
| `/query/queryAll` | GET | 分页查询大学汇总排名 |
| `/query/queryAllEcharts` | GET | 查询ECharts图表数据 |

## 🎯 核心特性

- **多源排名数据**: 整合QS、US News等多个权威排名数据源
- **专业排名**: 支持计算机科学等专业单独排名查询
- **多维筛选**: 按国家、大洲、排名范围等条件灵活筛选
- **可视化接口**: 提供ECharts图表数据接口，支持数据可视化展示
- **分页查询**: 支持大数据量分页查询，性能优秀
- **Knife4j文档**: 集成Knife4j接口文档，方便API测试
- **多数据源**: 支持动态多数据源配置

## 📁 项目结构

```
JOSP-choosePhdJava/
├── src/main/java/wo1261931780/choosecollegejava/
│   ├── controller/          # 控制器层
│   ├── service/             # 服务层
│   ├── mapper/              # 数据访问层
│   ├── entity/              # 实体类
│   ├── common/              # 通用类
│   └── ChooseCollegeJavaApplication.java
├── src/main/resources/
│   ├── application.yml       # 应用配置
│   └── static/              # 静态资源
├── pom.xml
├── README.md
└── SPEC.md
```

## 📝 提交规范

```
feat: 新功能
fix: 修复问题
docs: 文档更新
style: 代码格式调整
refactor: 重构
perf: 性能优化
test: 测试相关
chore: 构建/工具相关
```

## 👥 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 📄 许可证

本项目采用 AGPL-3.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📮 联系方式

项目维护者: JOSP Team

---

⭐ 如果这个项目对你有帮助，欢迎 Star 支持！
