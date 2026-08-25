package com.exam.controller;

import com.domain.entity.ExamRecord;
import com.domain.annotation.Audit;
import com.domain.entity.UserAnswer;
import com.domain.restful.RestResponse;
import com.exam.service.ExamRecordService;
import com.exam.service.ExamService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/examRecord")
public class ExamRecordController {

    @Autowired
    private ExamRecordService examRecordService;

    @Autowired
    private ExamService examService;

    @GetMapping({"/list", "/list/{userId}"})
    public RestResponse<List<ExamRecord>> listUserRecords(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable(required = false) Long userId) {
        Long currentUserId = currentUserId(jwt);
        if (currentUserId == null) {
            return RestResponse.fail("token中无userId");
        }
        if (userId != null && !currentUserId.equals(userId)) {
            return RestResponse.fail(403, "只能查看自己的考试记录");
        }
        List<ExamRecord> records = examRecordService.lambdaQuery()
                .eq(ExamRecord::getUserId, currentUserId)
                .orderByDesc(ExamRecord::getCreateTime)
                .list();
        Map<Long, String> examNames = records.isEmpty()
                ? Map.of()
                : examService.listByIds(records.stream()
                                .map(ExamRecord::getExamId)
                                .distinct()
                                .toList())
                        .stream()
                        .collect(Collectors.toMap(com.domain.entity.Exam::getId,
                                com.domain.entity.Exam::getTitle));
        records.forEach(record -> record.setExamName(examNames.get(record.getExamId())));
        return RestResponse.success(records);
    }

    @Audit("学生提交试卷")
    @PostMapping("/submit")
    public RestResponse<Void> submitExam(@AuthenticationPrincipal Jwt jwt,
                                         @RequestParam Long examId,
                                         @RequestBody List<UserAnswer> userAnswers) {
        Long userId = currentUserId(jwt);
        if (userId == null) {
            return RestResponse.fail("token中无userId");
        }
        examRecordService.submitExam(userId, examId,
                userAnswers == null ? List.of() : userAnswers);
        return RestResponse.success("交卷成功", null);
    }

    @Audit("教师批改主观题")
    @PostMapping("/gradeEssay")
    @PreAuthorize("@roleGuard.isTeacherOrAdmin(authentication)")
    public RestResponse<Boolean> gradeEssay(@RequestParam Long answerId,
                                             @RequestParam java.math.BigDecimal score) {
        return RestResponse.success(examRecordService.gradeEssay(answerId, score));
    }

    @GetMapping("/analysis")
    @PreAuthorize("@roleGuard.isTeacherOrAdmin(authentication)")
    public RestResponse<Map<String, Object>> analysis(@RequestParam(required = false) Long examId) {
        var query = examRecordService.lambdaQuery().eq(ExamRecord::getStatus, 2);
        if (examId != null) {
            query.eq(ExamRecord::getExamId, examId);
        }
        List<ExamRecord> records = query.list();
        List<BigDecimal> scores = records.stream()
                .map(ExamRecord::getTotalScore)
                .filter(scoreValue -> scoreValue != null)
                .toList();

        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = scores.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
        BigDecimal highest = scores.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal lowest = scores.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal passScore = BigDecimal.valueOf(60);
        if (examId != null) {
            com.domain.entity.Exam exam = examService.getById(examId);
            if (exam != null && exam.getPassScore() != null) {
                passScore = exam.getPassScore();
            }
        }
        BigDecimal analysisPassScore = passScore;
        long passed = scores.stream().filter(scoreValue -> scoreValue.compareTo(analysisPassScore) >= 0).count();
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("0-59", scores.stream().filter(scoreValue -> scoreValue.compareTo(BigDecimal.valueOf(60)) < 0).count());
        distribution.put("60-69", scores.stream().filter(scoreValue -> inRange(scoreValue, 60, 70)).count());
        distribution.put("70-79", scores.stream().filter(scoreValue -> inRange(scoreValue, 70, 80)).count());
        distribution.put("80-89", scores.stream().filter(scoreValue -> inRange(scoreValue, 80, 90)).count());
        distribution.put("90-100", scores.stream().filter(scoreValue -> scoreValue.compareTo(BigDecimal.valueOf(90)) >= 0).count());

        Map<String, Object> result = new HashMap<>();
        result.put("averageScore", average);
        result.put("passRate", scores.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(passed * 100.0 / scores.size()).setScale(2, RoundingMode.HALF_UP));
        result.put("highestScore", highest);
        result.put("lowestScore", lowest);
        result.put("distribution", distribution);
        result.put("total", scores.size());
        return RestResponse.success(result);
    }

    private boolean inRange(BigDecimal score, int minInclusive, int maxExclusive) {
        return score.compareTo(BigDecimal.valueOf(minInclusive)) >= 0
                && score.compareTo(BigDecimal.valueOf(maxExclusive)) < 0;
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null || jwt.getClaim("userId") == null) {
            return null;
        }
        Object value = jwt.getClaim("userId");
        return value instanceof Number ? ((Number) value).longValue() : Long.valueOf(String.valueOf(value));
    }
}
