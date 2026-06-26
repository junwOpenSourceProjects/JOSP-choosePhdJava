package com.choosephd.common;

import java.util.List;

/**
 * 分页结果包装类 — Controller 返列表分页的标准化 DTO。
 *
 * <p>字段：list / total / page / size / totalPages（衍生）。
 *
 * <p>跟 MyBatis-Plus {@code IPage<T>} 区别：本类不依赖 MyBatis-Plus，
 * 直接返 JSON 给前端，前端用 page+size 算 totalPages。
 *
 * <p>配套：
 * <ul>
 *   <li>{@link PageQuery} — 入参（前端传）</li>
 *   <li>{@link PageUtil} — 转 MyBatis-Plus Page 的工具类</li>
 * </ul>
 */
public class PageResult<T> {

    private List<T> list;
    private long total;
    private long page;
    private long size;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, long page, long size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResult<T> of(List<T> list, long total, long page, long size) {
        return new PageResult<>(list, total, page, size);
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
