package com.choosephd.controller.v1;

import com.choosephd.common.ApiResult;
import com.choosephd.entity.Subject;
import com.choosephd.service.SubjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
