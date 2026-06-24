# JOSP-choosePhdJava

PhD / 研究生选校平台后端服务。提供全球大学排名、学科排名、院校详情、选校清单等数据的 REST API。

---

## 技术栈

| 技术 | 版本 / 说明 |
| --- | --- |
| Java | 21 |
| Spring Boot | 3.5.5 |
| MyBatis-Plus | 3.5.9 |
| MySQL | 8.x |
| Flyway | 10.20.1 |
| Redis | 用于缓存 / Session |
| JWT | jjwt 0.12.6 |
| Maven | 构建工具 |

---

## 项目结构

```
JOSP-choosePhdJava/
├── src/main/java/com/choosephd/          # 后端业务代码
│   ├── controller/v1/                    # REST API 控制器
│   ├── service/                          # 业务逻辑
│   ├── repository/                       # MyBatis Mapper 接口
│   ├── entity/                           # 数据实体
│   ├── dto/                              # 请求/响应 DTO
│   ├── config/                           # 配置类
│   ├── security/                         # JWT / 拦截器
│   └── importer/                         # 启动时可选的数据导入
├── src/main/resources/
│   ├── db/migration/                     # Flyway 数据库迁移脚本
│   ├── mapper/                           # MyBatis XML
│   └── application.yml                   # 应用配置
├── src/test/                             # 单元测试
├── tools/menggy_crawler/                 # 梦奇大学排名爬虫
└── tools/data-scripts/                   # 数据清洗 / 导入脚本
```

---

## 快速开始

### 1. 环境准备

- JDK 21+
- Maven 3.9+
- MySQL 8.x
- Redis 7.x（可选，不启动时缓存相关功能会降级）

### 2. 创建数据库

```sql
CREATE DATABASE choosephd CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 配置

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/choosephd?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
```

### 4. 构建与运行

```bash
# 开发启动
mvn spring-boot:run

# 或打包后运行
mvn -q package -DskipTests
java -jar target/choosephd-server-1.0.0.jar
```

启动后访问：

- API Base: `http://localhost:56586/api/v1`
- Swagger UI: `http://localhost:56586/swagger-ui/index.html`
- API Docs: `http://localhost:56586/api-docs`

Flyway 会自动执行 `src/main/resources/db/migration/` 下的脚本完成建表。

### 5. 默认管理员账号

首次启动时，若 `user_account` 表中不存在配置的管理员账号，系统会自动创建：

```yaml
choosephd:
  admin:
    username: admin
    password: admin
```

即默认可用 **admin / admin** 登录。生产环境请务必在 `application.yml` 中修改该密码。

---

## 数据流程

本项目的数据来源是 [梦奇大学排名搜索](https://daxue.menggy.com/)。整体流程为：

```
梦奇网站  →  tools/menggy_crawler  →  menggy_rankings (原始库)
                                      ↓
                              tools/data-scripts
                                      ↓
                                choosephd (应用库)
                                      ↓
                          JOSP-choosePhdJava 提供 API
```

### 1. 爬取数据

进入爬虫目录，安装依赖：

```bash
cd tools/menggy_crawler
pip install -r requirements.txt
```

常用命令：

```bash
# 不登录，抓取所有学科榜单（分页）
python main.py --all --no-login --workers 10

# 仅重新抓取大学详情页
python main.py --details --force-details --no-login --workers 20

# 校验榜单完整性
python main.py --audit --no-login

# 修复缺失榜单
python main.py --backfill --no-login --workers 10
```

爬虫数据默认写入本地 MySQL 的 `menggy_rankings` 库。

### 2. 导入数据到 choosephd

```bash
cd tools/data-scripts
pip install pymysql

# 全量导入（大学 + 榜单 + 学科 + 排名）
python import_menggy_to_choosephd.py

# 仅导入/更新大学信息（跳过排名数据）
python import_menggy_to_choosephd.py --skip-rankings
```

### 3. 补充中文名（可选）

```bash
python translate_ranking_sources.py
python translate_subjects.py
```

---

## 主要 API

| 接口 | 说明 |
| --- | --- |
| `GET /api/v1/universities` | 大学列表 |
| `GET /api/v1/universities/{urlId}` | 大学详情（含官网/校训/建校时间/地址） |
| `GET /api/v1/universities/{urlId}/rankings` | 大学在各榜单的历史排名 |
| `GET /api/v1/sources` | 榜单源列表 |
| `GET /api/v1/sources/{id}` | 榜单源详情 |
| `GET /api/v1/sources/{id}/entries` | 榜单条目（大学排名） |
| `GET /api/v1/sources/{id}/years` | 榜单可用年份 |
| `GET /api/v1/subjects` | 学科列表 |
| `POST /api/v1/auth/login` | 登录 |
| `POST /api/v1/auth/register` | 注册 |
| `GET /api/v1/shortlist` | 选校清单 |

详细参数见 Swagger UI。

---

## 数据库说明

### 应用库 `choosephd`

| 表 | 说明 |
| --- | --- |
| `university` | 大学基础信息 + 官网/校训/建校时间/地址 |
| `ranking_source` | 榜单源（QS/THE/USNews/ARWU/RUR 等，含学科榜） |
| `subject` | 学科字典 |
| `ranking_entry` | 排名条目（大学在某榜单某年的排名） |
| `user_account` | 用户账号 |
| `user_shortlist` | 用户选校清单 |

### 爬虫原始库 `menggy_rankings`

| 表 | 说明 |
| --- | --- |
| `universities` | 大学基础信息 |
| `university_details` | 大学详情页原始数据 |
| `ranking_lists` | 榜单元数据 |
| `ranking_entries` | 排名条目原始数据 |
| `systems` | 榜单体系 |

---

## 开发说明

- 逻辑删除字段统一为 `deleted`，MyBatis-Plus 已配置全局逻辑删除。
- 所有实体主键使用数据库自增 ID。
- API 统一返回 `ApiResult<T>` 包装格式：`{ code, message, data }`。
- 生产环境请修改 `choosephd.jwt.secret`。

---

## License

MIT
