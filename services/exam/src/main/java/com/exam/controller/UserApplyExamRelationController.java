package com.exam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domain.restful.RestResponse;
import com.domain.dto.UserApplyExamRelationDto;
import com.domain.entity.relation.UserApplyExamRelation;
import com.domain.entity.Exam;
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

    /**
     * 当前登录用户获取已报考的考试详情列表，转换字段匹配前端要求
     */
    @GetMapping("/getUserAppliedExams")
    public RestResponse<Page<java.util.Map<String, Object>>> getUserAppliedExams(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Long userId = jwt.getClaim("userId");
        if (userId == null) {
            return RestResponse.fail("token中无userId");
        }

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserApplyExamRelation> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        Page<UserApplyExamRelation> relationPage = userApplyExamRelationService.page(new Page<>(pageNum, pageSize), queryWrapper);
        Page<java.util.Map<String, Object>> resultPage = new Page<>(relationPage.getCurrent(), relationPage.getSize(), relationPage.getTotal());

        java.util.List<Long> examIds = relationPage.getRecords().stream()
                .map(UserApplyExamRelation::getExamId)
                .collect(java.util.stream.Collectors.toList());

        if (examIds.isEmpty()) {
            resultPage.setRecords(new java.util.ArrayList<>());
            return RestResponse.success(resultPage);
        }

        java.util.List<Exam> exams = examService.listByIds(examIds);
        java.util.Map<Long, Exam> examMap = exams.stream().collect(java.util.stream.Collectors.toMap(Exam::getId, exam -> exam));

        java.util.List<java.util.Map<String, Object>> records = relationPage.getRecords().stream().map(relation -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            Exam exam = examMap.get(relation.getExamId());
            if (exam != null) {
                map.put("examId", String.valueOf(exam.getId()));
                map.put("examName", exam.getTitle());
                map.put("examDescription", exam.getIntroduce());
                map.put("beginTime", exam.getBeginTime());

                if (exam.getBeginTime() != null && exam.getDurationTime() != null) {
                    map.put("endTime", exam.getBeginTime().plusMinutes(exam.getDurationTime()));
                } else {
                    map.put("endTime", null);
                }

                map.put("lastTime", exam.getDurationTime() != null ? exam.getDurationTime() + "分钟" : "");
                map.put("maxUser", exam.getMaxUserNum());
                map.put("remainingUser", exam.getRestUserNum());
                map.put("passScore", exam.getPassScore());
                map.put("status", exam.getStatus());

                if (exam.getSecuritySetting() != null) {
                    map.put("maxReconnection", exam.getSecuritySetting().getMaxReconnectCount());
                    map.put("allowEarlyCommit", exam.getSecuritySetting().getAllowEarlySubmit() != null && exam.getSecuritySetting().getAllowEarlySubmit() ? 1 : 0);
                } else {
                    map.put("maxReconnection", 3);
                    map.put("allowEarlyCommit", 1);
                }

                map.put("creator", exam.getCreator() != null ? String.valueOf(exam.getCreator()) : "");
                map.put("createTime", exam.getCreateTime());
            }
            return map;
        }).filter(m -> !m.isEmpty()).collect(java.util.stream.Collectors.toList());

        resultPage.setRecords(records);
        return RestResponse.success(resultPage);
    }
}
