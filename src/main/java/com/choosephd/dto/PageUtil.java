package com.choosephd.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页转换工具类 — {@link PageQuery} ↔ MyBatis-Plus {@code Page<T>} 互转。
 *
 * <p>三个重载：
 * <ul>
 *   <li>{@code toPage(PageQuery)} — 用 PageQuery 自带 max=200 限制</li>
 *   <li>{@code toPage(page, size)} — 直接传 page/size，默认 max=200</li>
 *   <li>{@code toPage(page, size, maxSize)} — 自定义 max（特殊端点用）</li>
 * </ul>
 */
public class PageUtil {

    private PageUtil() {
    }

    public static <T> Page<T> toPage(PageQuery query) {
        return new Page<>(query.getPage(), query.getLimit());
    }

    public static <T> Page<T> toPage(Long page, Long size) {
        long p = page == null || page < 1 ? 1 : page;
        long s = size == null || size < 1 ? 20 : size;
        if (s > 200L) {
            s = 200L;
        }
        return new Page<>(p, s);
    }

    public static <T> Page<T> toPage(Long page, Long size, long maxSize) {
        long p = page == null || page < 1 ? 1 : page;
        long s = size == null || size < 1 ? 20 : size;
        if (s > maxSize) {
            s = maxSize;
        }
        return new Page<>(p, s);
    }
}
