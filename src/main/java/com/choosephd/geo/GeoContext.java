package com.choosephd.geo;

/**
 * 单次请求的国家上下文 — 由 {@code GeoFilter} 解析后写入 request attribute，
 * controller / service 按需通过 {@code GeoContextHolder.get()} 拿到。
 *
 * <p>为什么不用 ThreadLocal：HttpServletRequest 已经天然 per-request，
 * 用 attribute + Spring {@code @RequestScope} 比 ThreadLocal 更易清理。
 */
public class GeoContext {

    /** ISO 3166-1 alpha-2 国家代码（CN/US/GB/...） */
    private final String countryCode;

    /** 友好名称（"中国大陆" / "美国" / "未知"） */
    private final String countryName;

    /** IANA 时区（"Asia/Shanghai" / "America/New_York"） */
    private final String timezone;

    /** ISO 4217 货币代码（"CNY" / "USD" / "GBP"） */
    private final String currency;

    /** BCP 47 locale（"zh-CN" / "en-US"） */
    private final String locale;

    /** 客户端真实 IP（优先 X-Forwarded-For，取首段） */
    private final String ip;

    public GeoContext(String countryCode, String countryName, String timezone,
                      String currency, String locale, String ip) {
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.timezone = timezone;
        this.currency = currency;
        this.locale = locale;
        this.ip = ip;
    }

    public String getCountryCode() { return countryCode; }
    public String getCountryName() { return countryName; }
    public String getTimezone() { return timezone; }
    public String getCurrency() { return currency; }
    public String getLocale() { return locale; }
    public String getIp() { return ip; }

    public boolean isUnknown() { return CountryCode.UNKNOWN.getCode().equals(countryCode); }

    /** 默认上下文 — 用于过滤器未命中（如内部调用） */
    public static GeoContext unknown(String ip) {
        return new GeoContext(
                CountryCode.UNKNOWN.getCode(),
                CountryCode.UNKNOWN.getName(),
                CountryCode.UNKNOWN.getTimezone(),
                CountryCode.UNKNOWN.getCurrency(),
                CountryCode.UNKNOWN.getLocale(),
                ip);
    }
}
