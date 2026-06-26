package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.entity.Subject;
import com.choosephd.service.SubjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学科 controller — 单端点 GET / 返回所有学科列表（前端筛选用）。
 *
 * <p>无鉴权要求（学科列表公开访问）。学科数据由 init.sql 灌库，本 controller 只读。
 *
 * <p>Service：{@link com.choosephd.service.SubjectService}。
 */
@RestController
@RequestMapping("/api/v1/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    public ApiResult<List<Subject>> listSubjects(@RequestParam(required = false) String ownerOrg) {
        return ApiResult.ok(subjectService.listSubjects(ownerOrg));
    }
}
