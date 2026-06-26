package com.choosephd.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.choosephd.entity.Subject;
import com.choosephd.repository.SubjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 学科数据访问 service — 列表 + 单条查询。
 *
 * <p>学科是 ranking_entry.subject_id 关联的维度表，subject 表只读，无 admin 写操作
 * （学科列表由 init.sql 灌库）。
 *
 * <p>Controller 入口：{@link com.choosephd.controller.v1.SubjectController}。
 */
@Service
public class SubjectService {

    private final SubjectMapper subjectMapper;

    public SubjectService(SubjectMapper subjectMapper) {
        this.subjectMapper = subjectMapper;
    }

    public List<Subject> listSubjects(String ownerOrg) {
        QueryWrapper<Subject> wrapper = new QueryWrapper<>();
        wrapper.eq("active", 1).eq("deleted", 0);
        if (ownerOrg != null && !ownerOrg.isBlank()) {
            wrapper.eq("owner_org", ownerOrg.trim());
        }
        wrapper.orderByAsc("id");
        return subjectMapper.selectList(wrapper);
    }
}
