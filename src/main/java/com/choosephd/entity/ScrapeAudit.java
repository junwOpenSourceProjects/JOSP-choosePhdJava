package com.choosephd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 反爬虫审计日志 entity — 记录被 UA 黑名单 / 频率限制拦截的请求。
 *
 * <p>由 {@code AntiScrapeFilter} 与后续 {@code RateLimitFilter} 写入，
 * 数据全量保留（admin 页面按 IP/时间窗口聚合后告警）。
 *
 * <p>ID 雪花算法手动注入（{@link IdType#INPUT}），与项目其他主表保持一致。
 */
@TableName("scrape_audit")
public class ScrapeAudit {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String ip;

    private String userAgent;

    private String method;

    private String path;

    private Integer statusCode;

    private String rejectReason;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
