package com.choosephd.dto;

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
