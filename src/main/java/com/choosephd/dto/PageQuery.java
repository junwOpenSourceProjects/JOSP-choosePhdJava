package com.choosephd.dto;

/**
 * 分页入参 DTO — controller 接收前端的 page/size 参数。
 *
 * <p>防御性默认值：page < 1 → 1，size < 1 → 20，size > 200 → 200 (避免前端
 * 一次拉太多打爆数据库)。
 *
 * <p>配套：{@link PageUtil#toPage(PageQuery)} 转 MyBatis-Plus Page。
 */
public class PageQuery {

    private Long page;
    private Long size;

    public PageQuery() {
        this.page = 1L;
        this.size = 20L;
    }

    public Long getPage() {
        return page == null || page < 1 ? 1L : page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getSize() {
        return size == null || size < 1 ? 20L : size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getLimit() {
        Long s = getSize();
        return s > 200L ? 200L : s;
    }
}
