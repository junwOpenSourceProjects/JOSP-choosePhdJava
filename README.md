# choosephd-api · 后端服务

> choosePhd 院校选择系统 · 重做版后端。Spring Boot 3.3 + MyBatis-Plus 3.5 + MySQL 8 + Flyway 10 + JWT。

## 与旧版本差异

旧版本每个榜单各自一张表(7 张表)+ 复杂 DTO + 多 controller 分散导入。
新版本规范化到 5 张核心表 + 2 张字典 + 1 张应用表,详见 `src/main/resources/db/migration/V1__init_choosephd_schema.sql`。

## 快速开始

```bash
# 1. 确认 MySQL 已启动,root 账号可空密码连接
mysql -uroot -e "SELECT VERSION();"

# 2. 配置(若不是默认 root/空密码)
#    编辑 src/main/resources/application-dev.yml

# 3. 启动
./mvnw spring-boot:run

# 4. 健康检查
curl http://localhost:8080/api/v1/health

# 5. 登录(默认 admin/admin)
curl -X POST http://localhost:8080/api/v1/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"username":"admin","password":"admin"}'

# 6. 触发全量数据导入(异步可改)
curl -X POST http://localhost:8080/api/v1/admin/import/run \
     -H "Authorization: Bearer <token>"

# 7. 查看导入进度
curl http://localhost:8080/api/v1/admin/import/jobs \
     -H "Authorization: Bearer <token>"
```

## 数据源约定

读取 `${CHOOSEPHD_RAW_DIR:~/Desktop/ranking_data 备份 2}` 下的子目录,目录名 → 榜单 code 映射见
`com.choosephd.api.importing.RawDataScanner#mapSourceCode`。

## 端点清单(Phase 1 范围)

- `GET  /api/v1/health` — 健康检查(公开)
- `POST /api/v1/auth/login` — 登录拿 JWT(公开)
- `GET  /api/v1/dict/regions` — 地区字典(公开)
- `GET  /api/v1/dict/subjects` — 学科字典(公开)
- `GET  /api/v1/dict/sources` — 榜单元数据(公开)
- `POST /api/v1/admin/import/run` — 全量扫描并导入(需登录)
- `GET  /api/v1/admin/import/jobs` — 导入任务列表(需登录)

后续 Phase 2 将补充:`/api/v1/universities`、`/api/v1/rankings`、`/api/v1/trends`、`/api/v1/compare`。
