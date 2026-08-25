package com.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domain.dto.ExamQuestionRelationDto;
import com.domain.restful.RestResponse;
import com.domain.dto.ExamDto;
import com.domain.entity.Exam;
import com.domain.entity.attribute.QuestionBody;
import com.domain.vo.ExamQuestionRelationVo;
import com.exam.service.ExamService;
import com.exam.service.ExamQuestionRelationService;
import com.exam.service.UserApplyExamRelationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
public class ExamOptionController {
    private final ExamService examService;
    private final ExamQuestionRelationService examQuestionRelationService;
    private final UserApplyExamRelationService userApplyExamRelationService;

    public ExamOptionController(ExamService examService,
                                ExamQuestionRelationService examQuestionRelationService,
                                UserApplyExamRelationService userApplyExamRelationService) {
        this.examService = examService;
        this.examQuestionRelationService = examQuestionRelationService;
        this.userApplyExamRelationService = userApplyExamRelationService;
    }

    @PostMapping("/addExam")
    @PreAuthorize("@roleGuard.isTeacherOrAdmin(authentication)")
    public RestResponse<String> addExam(@AuthenticationPrincipal Jwt jwt, @RequestBody ExamDto dto){
        Long userId = jwt.getClaim("userId");
        if(userId==null)
            return RestResponse.fail("token中无userId");
        dto.setCreator(userId);

        String result= examService.insert(dto)? "添加成功":"添加失败";
        return RestResponse.success(result);
    }

    /**
     * description 临时
     * author zzq
     * date 2025/12/19 16:17
     * param * @param null
     * return
     */
    @GetMapping("/getExamById/{examId}")
    public RestResponse<Exam> getExamList(@PathVariable("examId") String examId){
        Long id=Long.parseLong(examId);
        Exam e= examService.getOne(new LambdaQueryWrapper<Exam>().eq(Exam::getId,id));
        return RestResponse.success(e);
    }

    /**
     * description 根据考试id获取考试题目（分页）
     * author zzq
     * date 2025/12/20 17:51
     * param
     * return
     */
    @PostMapping("/getExamQuestions")
    public RestResponse<Page<ExamQuestionRelationVo>> getExamQuestions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ExamQuestionRelationDto dto){
        if (jwt == null || jwt.getClaim("userId") == null) {
            return RestResponse.fail("token中无userId");
        }
        if (dto == null || dto.getExamId() == null) {
            return RestResponse.fail("考试ID不能为空");
        }
        Exam exam = examService.getById(dto.getExamId());
        if (exam == null) {
            return RestResponse.fail("考试不存在");
        }
        if (Boolean.FALSE.equals(exam.getStatus())) {
            return RestResponse.fail("考试已停用");
        }
        String role = jwt.getClaimAsString("role");
        boolean privileged = "teacher".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role);
        if (!privileged) {
            Long userId = jwt.getClaim("userId");
            if (!userApplyExamRelationService.checkExamApplyExist(userId, dto.getExamId())) {
                return RestResponse.fail(403, "未报名该考试");
            }
            if (exam.getBeginTime() == null || exam.getDurationTime() == null || exam.getDurationTime() <= 0) {
                return RestResponse.fail("考试时间配置不完整");
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = exam.getBeginTime().plusMinutes(exam.getDurationTime());
            if (now.isBefore(exam.getBeginTime()) || now.isAfter(endTime)) {
                return RestResponse.fail("当前不在考试时间内");
            }
        }
        Page<ExamQuestionRelationVo> page=examQuestionRelationService.getExamQuestionsByExamId(dto);
        // 学生端只需要题干和选项，正确答案与解析不能通过考试接口下发。
        page.getRecords().forEach(item -> item.setBody(sanitizeBody(item.getBody())));
        return RestResponse.success(page);
    }

    /**
     * 分页获取所有考试列表，转换字段以匹配前端要求
     */
    @GetMapping("/getPageExams")
    public RestResponse<Page<java.util.Map<String, Object>>> getPageExams(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        long safePageNum = Math.max(pageNum == null ? 1 : pageNum, 1);
        long safePageSize = Math.min(Math.max(pageSize == null ? 10 : pageSize, 1), 100);
        Page<Exam> examPage = examService.page(new Page<>(safePageNum, safePageSize),
                new LambdaQueryWrapper<Exam>().eq(Exam::getStatus, true).orderByDesc(Exam::getBeginTime));
        Page<java.util.Map<String, Object>> resultPage = new Page<>(examPage.getCurrent(), examPage.getSize(), examPage.getTotal());

        java.util.List<java.util.Map<String, Object>> records = examPage.getRecords().stream().map(exam -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
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
            return map;
        }).collect(java.util.stream.Collectors.toList());

        resultPage.setRecords(records);
        return RestResponse.success(resultPage);
    }

    private QuestionBody sanitizeBody(QuestionBody body) {
        if (body == null) {
            return null;
        }
        QuestionBody sanitized = new QuestionBody();
        sanitized.setStem(body.getStem());
        sanitized.setStemImg(body.getStemImg());
        sanitized.setOptions(body.getOptions());
        return sanitized;
    }
}
