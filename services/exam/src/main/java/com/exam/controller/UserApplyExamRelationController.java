package com.exam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domain.restful.RestResponse;
import com.domain.dto.UserApplyExamRelationDto;
import com.domain.entity.relation.UserApplyExamRelation;
import com.exam.service.ExamService;
import com.exam.service.UserApplyExamRelationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserApplyExamRelationController {
    private final UserApplyExamRelationService userApplyExamRelationService;
    private final ExamService examService;
    public UserApplyExamRelationController(UserApplyExamRelationService userApplyExamRelationService, ExamService examService) {
        this.userApplyExamRelationService = userApplyExamRelationService;
        this.examService = examService;
    }

    /**
     * description 考试报名
     * author zzq
     * date 2025/12/18 17:40
     */
    @PostMapping("/userApplyExam")
    public RestResponse<String> userApplyExam(@AuthenticationPrincipal Jwt jwt,
                                              @RequestBody UserApplyExamRelationDto userApplyExamRelationDto){
        Long userId = jwt.getClaim("userId");
        if(userId==null)
            return RestResponse.fail("token中无userId");
        examService.userApplyExam(userId,userApplyExamRelationDto.getExamId());
        //失败的情况都抛出了异常，要是能成功到这里，说明成功
        return RestResponse.success("报名成功");
    }

    /**
     * description 返回用户报名的考试列表，分页返回
     * author zzq
     * date 2025/12/18 19:58
     */
    //接口可能当前用户用，也可能管理员用，所以不从token中获取userId
    @GetMapping("/getUserApplyExamList")
    public RestResponse<Page<UserApplyExamRelation>> getUserApplyExamList(@RequestBody UserApplyExamRelationDto dto){
        Page<UserApplyExamRelation> list = userApplyExamRelationService.getList(dto);
        return RestResponse.success(list);
    }
}
