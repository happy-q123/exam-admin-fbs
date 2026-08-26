package com.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.domain.annotation.Audit;
import com.domain.dto.ExamDto;
import com.domain.dto.ExamQuestionRelationDto;
import com.domain.entity.Exam;
import com.domain.entity.relation.ExamQuestionRelation;
import com.domain.restful.RestResponse;
import com.exam.service.ExamQuestionRelationService;
import com.exam.service.ExamService;
import com.exam.service.QuestionService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 教师/管理员考试生命周期与组卷接口。 */
@RestController
@PreAuthorize("@roleGuard.isTeacherOrAdmin(authentication)")
public class ExamManagementController {
    private final ExamService examService;
    private final ExamQuestionRelationService relationService;
    private final QuestionService questionService;

    public ExamManagementController(ExamService examService, ExamQuestionRelationService relationService,
                                    QuestionService questionService) {
        this.examService = examService;
        this.relationService = relationService;
        this.questionService = questionService;
    }

    @GetMapping("/manage/list")
    public RestResponse<List<Exam>> list(@AuthenticationPrincipal Jwt jwt) {
        Long userId = currentUserId(jwt);
        boolean admin = isAdmin(jwt);
        LambdaQueryWrapper<Exam> query = new LambdaQueryWrapper<Exam>()
                .orderByDesc(Exam::getCreateTime);
        if (!admin) query.eq(Exam::getCreator, userId);
        return RestResponse.success(examService.list(query));
    }

    @Audit("创建考试")
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/manage")
    public RestResponse<Exam> create(@AuthenticationPrincipal Jwt jwt, @RequestBody ExamDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("考试配置不能为空");
        }
        Long userId = currentUserId(jwt);
        dto.setCreator(userId);
        Exam exam = examService.create(dto);
        if (dto.getQuestions() != null) {
            List<ExamQuestionRelation> entities = buildQuestionEntities(exam, dto.getQuestions());
            if (entities.isEmpty()) {
                throw new IllegalArgumentException("发布考试至少需要配置一道题目");
            }
            relationService.saveBatch(entities);
        }
        return RestResponse.success(exam);
    }

    @Audit("修改考试配置")
    @CacheEvict(value = "exam_security_cache", key = "#examId")
    @PutMapping("/manage/{examId}")
    public RestResponse<Boolean> update(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable("examId") Long examId,
                                        @RequestBody ExamDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("考试配置不能为空");
        }
        Exam exam = getOwnedExam(jwt, examId);
        if (exam.getBeginTime() != null && !LocalDateTime.now().isBefore(exam.getBeginTime())) {
            throw new IllegalStateException("考试开始后不能修改考试配置");
        }
        dto.setId(examId);
        dto.setCreator(exam.getCreator());
        dto.setRestUserNum(exam.getRestUserNum());
        dto.setCreateTime(exam.getCreateTime());
        dto.setLatestUpdateTime(LocalDateTime.now());
        Exam updated = dto.toExamForInsert();
        updated.setId(examId);
        updated.setLatestUpdateTime(LocalDateTime.now());
        return RestResponse.success(examService.updateById(updated));
    }

    @Audit("归档考试")
    @DeleteMapping("/manage/{examId}")
    public RestResponse<Boolean> archive(@AuthenticationPrincipal Jwt jwt, @PathVariable("examId") Long examId) {
        Exam exam = getOwnedExam(jwt, examId);
        exam.setStatus(false);
        exam.setLatestUpdateTime(LocalDateTime.now());
        return RestResponse.success(examService.updateById(exam));
    }

    @GetMapping("/manage/{examId}/questions")
    public RestResponse<List<ExamQuestionRelationDto>> listQuestions(@AuthenticationPrincipal Jwt jwt,
                                                                       @PathVariable("examId") Long examId) {
        getOwnedExam(jwt, examId);
        return RestResponse.success(relationService.lambdaQuery()
                .eq(ExamQuestionRelation::getExamId, examId)
                .orderByAsc(ExamQuestionRelation::getSeq)
                .list().stream().map(ExamQuestionRelationDto::toDto).toList());
    }

    @Audit("更新考试组卷")
    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/manage/{examId}/questions")
    public RestResponse<Boolean> replaceQuestions(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable("examId") Long examId,
                                                    @RequestBody List<ExamQuestionRelationDto> questions) {
        Exam exam = getOwnedExam(jwt, examId);
        if (exam.getBeginTime() != null && !LocalDateTime.now().isBefore(exam.getBeginTime())) {
            throw new IllegalStateException("考试开始后不能修改组卷");
        }
        List<ExamQuestionRelation> entities = buildQuestionEntities(exam, questions);
        relationService.remove(new LambdaQueryWrapper<ExamQuestionRelation>()
                .eq(ExamQuestionRelation::getExamId, examId));
        if (entities.isEmpty()) return RestResponse.success(true);
        return RestResponse.success(relationService.saveBatch(entities));
    }

    private List<ExamQuestionRelation> buildQuestionEntities(Exam exam,
                                                              List<ExamQuestionRelationDto> questions) {
        if (questions == null || questions.isEmpty()) return List.of();
        Set<Long> questionIds = questions.stream().filter(item -> item != null && item.getQuestionId() != null)
                .map(ExamQuestionRelationDto::getQuestionId).collect(Collectors.toSet());
        if (questionIds.size() != questions.size()) {
            throw new IllegalArgumentException("组卷题目不能为空且不能重复");
        }
        long existingCount = questionService.lambdaQuery().in(com.domain.entity.Question::getId, questionIds).count();
        if (existingCount != questionIds.size()) {
            throw new IllegalArgumentException("组卷包含不存在或已删除的题目");
        }
        Set<Integer> sequenceNumbers = new HashSet<>();
        List<ExamQuestionRelation> entities = questions.stream().map(item -> {
            if (item == null || item.getQuestionId() == null) throw new IllegalArgumentException("组卷题目不能为空");
            BigDecimal score = item.getScore() == null ? BigDecimal.ONE : item.getScore();
            if (score.signum() <= 0 || score.compareTo(BigDecimal.valueOf(1000)) > 0) {
                throw new IllegalArgumentException("每道题分值必须在 0 到 1000 之间且不能为 0");
            }
            int sequence = item.getSeq() == null ? questions.indexOf(item) + 1 : item.getSeq();
            if (sequence <= 0 || !sequenceNumbers.add(sequence)) {
                throw new IllegalArgumentException("组卷题号必须为正整数且不能重复");
            }
            return ExamQuestionRelation.builder()
                    .examId(exam.getId())
                    .questionId(item.getQuestionId())
                    .score(score)
                    .seq(sequence)
                    .overrideProps(item.getOverrideProps())
                    .build();
        }).toList();
        BigDecimal totalScore = entities.stream().map(ExamQuestionRelation::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (exam.getPassScore() != null && exam.getPassScore().compareTo(totalScore) > 0) {
            throw new IllegalArgumentException("及格分不能高于试卷总分（当前总分：" + totalScore.stripTrailingZeros().toPlainString() + "）");
        }
        return entities;
    }

    private Exam getOwnedExam(Jwt jwt, Long examId) {
        Exam exam = examService.getById(examId);
        if (exam == null) throw new java.util.NoSuchElementException("考试不存在");
        if (!isAdmin(jwt) && !currentUserId(jwt).equals(exam.getCreator())) {
            throw new AccessDeniedException("无权操作该考试");
        }
        return exam;
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null || jwt.getClaim("userId") == null) throw new IllegalArgumentException("登录用户不存在");
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
