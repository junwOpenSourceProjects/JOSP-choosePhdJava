package com.choosephd.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.choosephd.entity.ScrapeAudit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 反爬虫审计日志 mapper — 写入由 {@code AntiScrapeFilter} / {@code RateLimitFilter}
 * 直接调 {@code insert} 完成，本接口只读。
 *
 * <p>关键查询：
 * <ul>
 *   <li>{@link #countByIpSince(String, LocalDateTime)} — 单 IP 时间窗口计数，触发告警</li>
 *   <li>{@link #listRecent(int)} — admin 审计页面分页展示</li>
 * </ul>
 */
@Mapper
public interface ScrapeAuditMapper extends BaseMapper<ScrapeAudit> {

    /**
     * 统计某 IP 自指定时间以来的拦截次数。
     *
     * @param ip        客户端 IP
     * @param since     起始时间（含）
     * @return          拦截次数
     */
    @Select("SELECT COUNT(*) FROM scrape_audit WHERE ip = #{ip} AND created_at >= #{since}")
    long countByIpSince(@Param("ip") String ip, @Param("since") LocalDateTime since);

    /**
     * 查询最近的拦截记录，按时间倒序。
     *
     * @param limit     返回条数（admin 页面默认 50）
     * @return          拦截记录列表
     */
    @Select("SELECT * FROM scrape_audit ORDER BY created_at DESC LIMIT #{limit}")
    java.util.List<ScrapeAudit> listRecent(@Param("limit") int limit);
}
