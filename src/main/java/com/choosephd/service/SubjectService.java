package com.choosephd.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.choosephd.entity.Subject;
import com.choosephd.repository.SubjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

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
