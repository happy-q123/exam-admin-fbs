package com.exam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domain.restful.RestResponse;
import com.domain.dto.UserApplyExamRelationDto;
import com.domain.entity.relation.UserApplyExamRelation;
import com.domain.entity.Exam;
import com.exam.service.ExamService;
import com.exam.service.ExamQuestionRelationService;
import com.exam.service.UserApplyExamRelationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
@RestController
public class UserApplyExamRelationController {
    private final UserApplyExamRelationService userApplyExamRelationService;
    private final ExamService examService;
    private final ExamQuestionRelationService examQuestionRelationService;
    public UserApplyExamRelationController(UserApplyExamRelationService userApplyExamRelationService,
                                           ExamService examService,
                                           ExamQuestionRelationService examQuestionRelationService) {
        this.userApplyExamRelationService = userApplyExamRelationService;
        this.examService = examService;
        this.examQuestionRelationService = examQuestionRelationService;
    }

    /**
     * description 考试报名
     * author zzq
     * date 2025/12/18 17:40
     */
    @PostMapping("/userApplyExam")
    public RestResponse<String> userApplyExam(@AuthenticationPrincipal Jwt jwt,
                                              @RequestBody UserApplyExamRelationDto userApplyExamRelationDto){
        Long userId = currentUserId(jwt);
        if(userId==null)
            return RestResponse.fail("token中无userId");
        if (userApplyExamRelationDto == null || userApplyExamRelationDto.getExamId() == null) {
            return RestResponse.fail("考试ID不能为空");
        }
        examService.userApplyExam(userId,userApplyExamRelationDto.getExamId());
        //失败的情况都抛出了异常，要是能成功到这里，说明成功
        return RestResponse.success("报名成功");
    }

    /**
     * description 返回用户报名的考试列表，分页返回
     * author zzq
     * date 2025/12/18 19:58
     */
    @GetMapping("/getUserApplyExamList")
    @PreAuthorize("@roleGuard.isTeacherOrAdmin(authentication)")
    public RestResponse<Page<UserApplyExamRelation>> getUserApplyExamList(@AuthenticationPrincipal Jwt jwt,
                                                                           @ModelAttribute UserApplyExamRelationDto dto){
        if (dto == null) {
            dto = new UserApplyExamRelationDto();
        }
        Long currentUserId = currentUserId(jwt);
        if (currentUserId == null) {
            return RestResponse.fail("token中无userId");
        }
        if (!isAdmin(jwt)) {
            if (dto.getExamId() == null) {
                return RestResponse.fail("教师查询报名名单时必须指定考试ID");
            }
            Exam exam = examService.getById(dto.getExamId());
            if (exam == null || !currentUserId.equals(exam.getCreator())) {
                return RestResponse.fail(403, "无权查看该考试的报名名单");
            }
        }
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
        Long userId = currentUserId(jwt);
        if (userId == null) {
            return RestResponse.fail("token中无userId");
        }

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserApplyExamRelation> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        long safePageNum = Math.max(pageNum == null ? 1 : pageNum, 1);
        long safePageSize = Math.min(Math.max(pageSize == null ? 10 : pageSize, 1), 100);
        Page<UserApplyExamRelation> relationPage = userApplyExamRelationService.page(new Page<>(safePageNum, safePageSize), queryWrapper);
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
        java.util.Map<Long, BigDecimal> totalScoreMap = examQuestionRelationService.lambdaQuery()
                .in(com.domain.entity.relation.ExamQuestionRelation::getExamId, examIds)
                .list().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        com.domain.entity.relation.ExamQuestionRelation::getExamId,
                        java.util.stream.Collectors.mapping(
                                item -> item.getScore() == null ? BigDecimal.ZERO : item.getScore(),
                                java.util.stream.Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

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
                map.put("totalScore", totalScoreMap.getOrDefault(exam.getId(), BigDecimal.ZERO));
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

    private Long currentUserId(Jwt jwt) {
        if (jwt == null || jwt.getClaim("userId") == null) return null;
        Object value = jwt.getClaim("userId");
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private boolean isAdmin(Jwt jwt) {
        if (jwt == null) return false;
        String role = jwt.getClaimAsString("role");
        if ("admin".equalsIgnoreCase(role) || "ROLE_admin".equalsIgnoreCase(role)) return true;
        Object roles = jwt.getClaim("roles");
        if (roles instanceof java.util.Collection<?> collection
                && collection.stream().map(String::valueOf)
                .anyMatch(item -> "admin".equalsIgnoreCase(item.replaceFirst("^ROLE_", "")))) {
            return true;
        }
        Object authorities = jwt.getClaim("authorities");
        return authorities instanceof java.util.Collection<?> collection
                && collection.stream().map(String::valueOf)
                .anyMatch(item -> "admin".equalsIgnoreCase(item.replaceFirst("^ROLE_", "")));
    }
}
