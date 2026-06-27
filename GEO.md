# GEO (Geographic) 适配说明

**Session**: 2026-06-27 Day 1 evening

## 已实现

1. **CountryCode 枚举** — 12 个目标国 + UNKNOWN，每项含 `code/name/timezone/currency/locale`
2. **GeoContext / GeoContextHolder** — 单次请求国家上下文 + 静态访问点
3. **GeoFilter** (order 260) — 解析 `X-Country` > `CF-IPCountry` > 本机=CN > 兜底 UNKNOWN
4. **GeoDebugController** — `GET /api/v1/geo/me` 供前端联调
5. **WebConfig** — `/api/v1/geo/**` 不需要 token

## 端到端 verify 结果

```
CC   | name   | timezone            | currency
-----|--------|---------------------|---------
US   | 美国   | America/New_York    | USD
GB   | 英国   | Europe/London       | GBP
JP   | 日本   | Asia/Tokyo          | JPY
HK   | 香港   | Asia/Hong_Kong      | HKD
SG   | 新加坡 | Asia/Singapore      | SGD
CA   | 加拿大 | America/Toronto     | CAD
AU   | 澳大利亚 | Australia/Sydney  | AUD
DE   | 德国   | Europe/Berlin       | EUR
FR   | 法国   | Europe/Paris        | EUR
NL   | 荷兰   | Europe/Amsterdam    | EUR
CH   | 瑞士   | Europe/Zurich       | CHF
XX   | 未知   | Asia/Shanghai       | CNY   (无效代码兜底)
本机 | 中国大陆 | Asia/Shanghai      | CNY   (无 header fallback)
```

## 后续 controller 集成（待用户拍板）

### 优先级 P1：榜单权重差异化

**需求**: US IP 用户访问 /api/v1/universities 时，US News 主榜排第一；
CN IP 用户访问时，QS 主榜排第一。

**实现路径**:
```java
// UniversityController.list() 添加
String defaultSortType = GeoContextHolder.get().getCountryCode().equals("US")
    ? "usnews" : "qs";

if (sortType == null) sortType = defaultSortType;
```

**权衡**:
- ✅ 提升转化率（US 学生首选 US News 视角）
- ❌ 后期用户投诉"为什么榜单顺序变了"需文档解释
- ❌ 测试矩阵扩展（11 国 × N 业务场景）

### 优先级 P2：按国家隐藏榜单

**需求**: GB IP 不展示 THE 榜单（THE 英国榜单冗余），HK IP 强制展示 HK 院校专题。

**实现**: UniversityQueryService 加 `applyGeoFilter(QueryWrapper, CountryCode)`，
从查询 SQL 加 WHERE 条件。

### 优先级 P3：截止日期时区

**需求**: US 用户看到 `Dec 15, 2026 11:59 PM EST`，CN 用户看到 `2026-12-16 11:59 AM (北京时间)`。

**实现**: DTO `deadline_at` 改为 ISO Z 时间戳 + 前端 Intl.DateTimeFormat + timezone。
**已有 format utils 直接复用** (跟 JOSP-yuq 06-14 立 formatDate 模式同源)。

### 优先级 P4：货币

**需求**: 文学院费 / 生活费按用户货币显示。

**实现**: 后端只存 USD 基准金额，DTO 加 `displayAmount` + `displayCurrency` 字段，
前端按 Intl.NumberFormat 格式化。

### 优先级 P5：按国家差异化限流

**需求**: US IP 90 req/min (低风险), CN IP 30 req/min (高风险)。

**实现**: RateLimitFilter 读 GeoContextHolder，桶 key 加 countryCode 维度。
**注意**: 已 commit 的 RateLimitFilter 用了纯 IP 桶，要兼容升级。

## 反模式

❌ **不要做 IP 黑名单**: 同 IP 段多家用户共用（中国移动 NAT），误杀率高
❌ **不要做 Geo 跳转**: 留学站用户跨国 VPN 常见，跳转会丢失上下文
❌ **不要存 Geo 到 DB**: 每次请求重新解析，无需持久化

## nginx 部署示例（生产）

```nginx
# /etc/nginx/conf.d/choosephd.conf
server {
    listen 80;
    server_name choosePhd.com;

    # nginx + Cloudflare 共存
    set $country "XX";
    if ($http_cf_ipcountry) { set $country $http_cf_ipcountry; }

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header X-Country $country;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header Host $host;
    }
}
```

## 不引入 MaxMind GeoLite2 的理由

- 国内流量占 90%+，nginx + CF 已能 99% 覆盖
- MaxMind mmdb 文件 60MB+，启动加载慢
- 准确率：CF IP 库 95% / MaxMind 99%（对国内站差异不大）
- 未来如需自部署：单独引入 `com.maxmind.geoip2:geoip2:4.2.0` + GeoLite2-Country.mmdb，
  新增 `GeoIpService`，GeoFilter 优先级 4 调它
