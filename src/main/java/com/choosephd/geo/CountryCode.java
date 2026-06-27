package com.choosephd.geo;

/**
 * ISO 3166-1 alpha-2 国家代码枚举（按 PhD 留学目标国家优先级排序）。
 *
 * <p>本项目不引入完整枚举（190+ 国家），仅含实际影响榜单权重的目标国：
 * 中国大陆学生最关心的目标国家 — 美/英/港/新/日/加/澳/欧陆/中国大陆自身。
 *
 * <p>服务端 fallback：解析失败 / 私有 IP / 未知国家 → {@link #UNKNOWN}，
 * 前端拿到 {@code UNKNOWN} 时使用默认配置（中国大陆视角的排序）。
 */
public enum CountryCode {

    CN("CN", "中国大陆", "Asia/Shanghai", "CNY", "zh-CN"),
    US("US", "美国",     "America/New_York", "USD", "en-US"),
    GB("GB", "英国",     "Europe/London",    "GBP", "en-GB"),
    HK("HK", "香港",     "Asia/Hong_Kong",   "HKD", "zh-HK"),
    SG("SG", "新加坡",   "Asia/Singapore",   "SGD", "en-SG"),
    JP("JP", "日本",     "Asia/Tokyo",       "JPY", "ja-JP"),
    CA("CA", "加拿大",   "America/Toronto",  "CAD", "en-CA"),
    AU("AU", "澳大利亚", "Australia/Sydney", "AUD", "en-AU"),
    DE("DE", "德国",     "Europe/Berlin",    "EUR", "de-DE"),
    FR("FR", "法国",     "Europe/Paris",     "EUR", "fr-FR"),
    NL("NL", "荷兰",     "Europe/Amsterdam", "EUR", "nl-NL"),
    CH("CH", "瑞士",     "Europe/Zurich",    "CHF", "de-CH"),
    UNKNOWN("XX", "未知", "Asia/Shanghai",    "CNY", "zh-CN");

    private final String code;
    private final String name;
    private final String timezone;
    private final String currency;
    private final String locale;

    CountryCode(String code, String name, String timezone, String currency, String locale) {
        this.code = code;
        this.name = name;
        this.timezone = timezone;
        this.currency = currency;
        this.locale = locale;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getTimezone() { return timezone; }
    public String getCurrency() { return currency; }
    public String getLocale() { return locale; }

    /**
     * 从 ISO 代码解析，未知代码回退 UNKNOWN。
     */
    public static CountryCode fromCode(String code) {
        if (code == null || code.isEmpty()) return UNKNOWN;
        for (CountryCode c : values()) {
            if (c.code.equalsIgnoreCase(code)) return c;
        }
        return UNKNOWN;
    }
}
