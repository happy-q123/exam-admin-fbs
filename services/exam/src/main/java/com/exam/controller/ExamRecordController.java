package com.exam.controller;

import com.domain.entity.ExamRecord;
import com.domain.annotation.Audit;
import com.domain.entity.UserAnswer;
import com.domain.entity.relation.ExamQuestionRelation;
import com.domain.restful.RestResponse;
import com.exam.service.ExamRecordService;
import com.exam.service.ExamService;
import com.exam.service.UserAnswerService;
import com.exam.service.ExamQuestionRelationService;
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

    @Autowired
    private UserAnswerService userAnswerService;

    @Autowired
    private ExamQuestionRelationService examQuestionRelationService;

    @GetMapping({"/list", "/list/{userId}"})
    public RestResponse<List<ExamRecord>> listUserRecords(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable(value = "userId", required = false) Long userId) {
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
                                         @RequestParam("examId") Long examId,
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
    public RestResponse<Boolean> gradeEssay(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam("answerId") Long answerId,
                                             @RequestParam("score") java.math.BigDecimal score) {
        UserAnswer answer = userAnswerService.getById(answerId);
        if (answer == null || answer.getRecordId() == null) {
            return RestResponse.fail("答题记录不存在");
        }
        ExamRecord record = examRecordService.getById(answer.getRecordId());
        if (record == null || !canManageExam(jwt, record.getExamId())) {
            return RestResponse.fail(403, "无权批改该考试");
        }
        return RestResponse.success(examRecordService.gradeEssay(answerId, score));
    }

    /**
     * 返回待批改的主观题，供教师工作台批量处理。
     */
    @GetMapping("/pendingEssays")
    @PreAuthorize("@roleGuard.isTeacherOrAdmin(authentication)")
    public RestResponse<List<Map<String, Object>>> pendingEssays(@AuthenticationPrincipal Jwt jwt) {
        List<Long> managedExamIds = examService.lambdaQuery()
                .select(com.domain.entity.Exam::getId)
                .eq(!isAdmin(jwt), com.domain.entity.Exam::getCreator, currentUserId(jwt))
                .list().stream().map(com.domain.entity.Exam::getId).toList();
        if (managedExamIds.isEmpty()) return RestResponse.success(List.of());

        Map<Long, ExamRecord> records = examRecordService.lambdaQuery()
                .eq(ExamRecord::getStatus, 1)
                .in(ExamRecord::getExamId, managedExamIds)
                .list().stream().collect(Collectors.toMap(ExamRecord::getRecordId, item -> item));
        if (records.isEmpty()) return RestResponse.success(List.of());

        Map<Long, String> examTitles = examService.listByIds(records.values().stream()
                        .map(ExamRecord::getExamId).distinct().toList()).stream()
                .collect(Collectors.toMap(com.domain.entity.Exam::getId,
                        com.domain.entity.Exam::getTitle));
        Map<String, BigDecimal> maxScores = examQuestionRelationService.lambdaQuery()
                .in(ExamQuestionRelation::getExamId, records.values().stream()
                        .map(ExamRecord::getExamId).distinct().toList())
                .list().stream().collect(Collectors.toMap(
                        relation -> relation.getExamId() + ":" + relation.getQuestionId(),
                        ExamQuestionRelation::getScore, (first, second) -> first));

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        userAnswerService.lambdaQuery().in(UserAnswer::getRecordId, records.keySet())
                .isNull(UserAnswer::getIsCorrect).orderByAsc(UserAnswer::getCreateTime)
                .list().forEach(answer -> {
                    ExamRecord record = records.get(answer.getRecordId());
                    if (record == null) return;
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("answerId", answer.getAnswerId());
                    item.put("recordId", answer.getRecordId());
                    item.put("examId", record.getExamId());
                    item.put("examTitle", examTitles.get(record.getExamId()));
                    item.put("userId", record.getUserId());
                    item.put("questionId", answer.getQuestionId());
                    item.put("userAnswer", answer.getUserAnswer());
                    item.put("maxScore", maxScores.get(record.getExamId() + ":" + answer.getQuestionId()));
                    item.put("submitTime", answer.getCreateTime());
                    result.add(item);
                });
        return RestResponse.success(result);
    }

    @GetMapping("/analysis")
    @PreAuthorize("@roleGuard.isTeacherOrAdmin(authentication)")
    public RestResponse<Map<String, Object>> analysis(@AuthenticationPrincipal Jwt jwt,
                                                      @RequestParam(value = "examId", required = false) Long examId) {
        Long userId = currentUserId(jwt);
        if (userId == null) {
            return RestResponse.fail("token中无userId");
        }
        boolean admin = isAdmin(jwt);
        List<com.domain.entity.Exam> managedExams;
        if (examId != null) {
            com.domain.entity.Exam exam = examService.getById(examId);
            if (exam == null) {
                return RestResponse.fail("考试不存在");
            }
            if (!admin && !userId.equals(exam.getCreator())) {
                return RestResponse.fail(403, "无权查看该考试的成绩分析");
            }
            managedExams = List.of(exam);
        } else {
            managedExams = examService.lambdaQuery()
                    .select(com.domain.entity.Exam::getId, com.domain.entity.Exam::getPassScore)
                    .eq(!admin, com.domain.entity.Exam::getCreator, userId)
                    .list();
        }

        java.util.Set<Long> managedExamIds = managedExams.stream()
                .map(com.domain.entity.Exam::getId)
                .collect(Collectors.toSet());
        var query = examRecordService.lambdaQuery().eq(ExamRecord::getStatus, 2);
        if (managedExamIds.isEmpty()) {
            query.eq(ExamRecord::getExamId, -1L);
        } else {
            query.in(ExamRecord::getExamId, managedExamIds);
        }
        List<ExamRecord> records = query.list();
        Map<Long, BigDecimal> passScores = managedExams.stream()
                .collect(Collectors.toMap(com.domain.entity.Exam::getId,
                        exam -> exam.getPassScore() == null ? BigDecimal.valueOf(60) : exam.getPassScore()));
        List<BigDecimal> scores = records.stream()
                .map(ExamRecord::getTotalScore)
                .filter(scoreValue -> scoreValue != null)
                .toList();

        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = scores.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
        BigDecimal highest = scores.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal lowest = scores.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        long passed = records.stream()
                .filter(record -> record.getTotalScore() != null)
                .filter(record -> record.getTotalScore()
                        .compareTo(passScores.getOrDefault(record.getExamId(), BigDecimal.valueOf(60))) >= 0)
                .count();
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
        result.put("examCount", managedExamIds.size());
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

    private boolean canManageExam(Jwt jwt, Long examId) {
        if (isAdmin(jwt)) return true;
        Long userId = currentUserId(jwt);
        com.domain.entity.Exam exam = examService.getById(examId);
        return userId != null && exam != null && userId.equals(exam.getCreator());
    }

    private boolean isAdmin(Jwt jwt) {
        if (jwt == null) return false;
        Object roles = jwt.getClaim("roles");
        if (roles instanceof java.util.Collection<?> collection
                && collection.stream().map(String::valueOf).anyMatch(item -> "admin".equalsIgnoreCase(item.replace("ROLE_", "")))) {
            return true;
        }
        String role = jwt.getClaimAsString("role");
        if ("admin".equalsIgnoreCase(role) || "ROLE_admin".equalsIgnoreCase(role)) return true;
        Object authorities = jwt.getClaim("authorities");
        return authorities instanceof java.util.Collection<?> collection
                && collection.stream().map(String::valueOf)
                .anyMatch(item -> "admin".equalsIgnoreCase(item.replaceFirst("^ROLE_", "")));
    }
}
