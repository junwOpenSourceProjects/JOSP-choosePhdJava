package com.choosephd.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public class PageUtil {

    private PageUtil() {
    }

    public static <T> Page<T> toPage(PageQuery query) {
        return new Page<>(query.getPage(), query.getLimit());
    }

    public static <T> Page<T> toPage(Long page, Long size) {
        long p = page == null || page < 1 ? 1 : page;
        long s = size == null || size < 1 ? 20 : size;
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
