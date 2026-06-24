package com.choosephd.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("university_alias")
public class UniversityAlias {

    @TableId
    private String aliasUrlId;
    private String targetUrlId;
    private LocalDateTime createdAt;

    public String getAliasUrlId() {
        return aliasUrlId;
    }

    public void setAliasUrlId(String aliasUrlId) {
        this.aliasUrlId = aliasUrlId;
    }

    public String getTargetUrlId() {
        return targetUrlId;
    }

    public void setTargetUrlId(String targetUrlId) {
        this.targetUrlId = targetUrlId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
