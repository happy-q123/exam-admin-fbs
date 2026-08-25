package com.exam.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.dto.QuestionDto;
import com.domain.entity.ErrorBook;
import com.domain.entity.Exam;
import com.domain.entity.ExamRecord;
import com.domain.entity.Question;
import com.domain.entity.UserAnswer;
import com.domain.entity.attribute.QuestionBody;
import com.domain.entity.relation.ExamQuestionRelation;
import com.domain.enums.QuestionTypeEnum;
import com.exam.mapper.ExamRecordMapper;
import com.exam.service.ErrorBookService;
import com.exam.service.ExamQuestionRelationService;
import com.exam.service.ExamRecordService;
import com.exam.service.ExamService;
import com.exam.service.UserAnswerService;
import com.exam.service.UserApplyExamRelationService;
import com.exam.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 考试记录服务实现类。
 *
 * <p>交卷时以考试题目关系表中的分值为准，并通过题库服务取得正确答案进行客观题判分。
 * 主观题保留待批改状态，避免使用固定答案或固定分值造成成绩失真。</p>
 */
@Service
public class ExamRecordServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord>
        implements ExamRecordService {

    @Autowired
    private UserAnswerService userAnswerService;

    @Autowired
    private ErrorBookService errorBookService;

    @Autowired
    private ExamQuestionRelationService examQuestionRelationService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private UserApplyExamRelationService userApplyExamRelationService;

    @Autowired
    private ExamService examService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitExam(Long userId, Long examId, List<UserAnswer> submittedAnswers) {
        if (userId == null || examId == null) {
            throw new IllegalArgumentException("用户ID和考试ID不能为空");
        }

        Exam exam = examService.getById(examId);
        if (exam == null) {
            throw new IllegalArgumentException("考试不存在");
        }
        if (Boolean.FALSE.equals(exam.getStatus())) {
            throw new IllegalArgumentException("考试已停用");
        }
        if (exam.getBeginTime() == null || exam.getDurationTime() == null || exam.getDurationTime() <= 0) {
            throw new IllegalArgumentException("考试时间配置不完整");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime examEndTime = exam.getBeginTime().plusMinutes(exam.getDurationTime());
        if (now.isBefore(exam.getBeginTime())) {
            throw new IllegalArgumentException("考试尚未开始");
        }
        // 给客户端倒计时和网络传输预留 30 秒容差，避免自动交卷因请求略晚被拒绝。
        if (now.isAfter(examEndTime.plusSeconds(30))) {
            throw new IllegalArgumentException("考试已结束");
        }
        if (exam.getSecuritySetting() != null
                && Boolean.FALSE.equals(exam.getSecuritySetting().getAllowEarlySubmit())
                && now.isBefore(examEndTime)) {
            throw new IllegalArgumentException("本场考试不允许提前交卷");
        }
        if (!userApplyExamRelationService.checkExamApplyExist(userId, examId)) {
            throw new IllegalArgumentException("未报名该考试");
        }
        if (lambdaQuery().eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getExamId, examId)
                .exists()) {
            throw new IllegalStateException("该考试已经提交过试卷");
        }

        List<ExamQuestionRelation> relations = examQuestionRelationService.lambdaQuery()
                .eq(ExamQuestionRelation::getExamId, examId)
                .orderByAsc(ExamQuestionRelation::getSeq)
                .list();
        if (relations.isEmpty()) {
            throw new IllegalStateException("该考试尚未配置题目");
        }

        Map<Long, ExamQuestionRelation> relationByQuestion = relations.stream()
                .collect(Collectors.toMap(ExamQuestionRelation::getQuestionId, relation -> relation,
                        (first, second) -> first));
        List<Long> questionIds = relations.stream()
                .map(ExamQuestionRelation::getQuestionId)
                .distinct()
                .toList();
        Map<Long, QuestionDto> questionById = questionService.listByIds(questionIds).stream()
                .map(QuestionDto::toDto)
                .collect(Collectors.toMap(QuestionDto::getId, question -> question, (first, second) -> first));

        Map<Long, UserAnswer> submittedByQuestion = new HashMap<>();
        if (submittedAnswers != null) {
            for (UserAnswer answer : submittedAnswers) {
                if (answer == null || answer.getQuestionId() == null) {
                    throw new IllegalArgumentException("答题明细缺少题目ID");
                }
                if (!relationByQuestion.containsKey(answer.getQuestionId())) {
                    throw new IllegalArgumentException("提交了不属于本场考试的题目");
                }
                if (submittedByQuestion.put(answer.getQuestionId(), answer) != null) {
                    throw new IllegalArgumentException("同一道题不能重复提交");
                }
            }
        }

        ExamRecord record = new ExamRecord();
        record.setUserId(userId);
        record.setExamId(examId);
        record.setStartTime(now);
        record.setEndTime(now);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setTotalScore(BigDecimal.ZERO);
        record.setStatus(1);
        save(record);

        List<UserAnswer> answersToSave = new ArrayList<>(relations.size());
        BigDecimal totalScore = BigDecimal.ZERO;
        boolean hasPendingSubjective = false;

        for (ExamQuestionRelation relation : relations) {
            QuestionDto question = questionById.get(relation.getQuestionId());
            if (question == null || question.getBody() == null) {
                throw new IllegalStateException("题目不存在：" + relation.getQuestionId());
            }

            UserAnswer answer = submittedByQuestion.get(relation.getQuestionId());
            if (answer == null) {
                answer = new UserAnswer();
                answer.setQuestionId(relation.getQuestionId());
                answer.setUserAnswer("");
            }
            answer.setRecordId(record.getRecordId());
            answer.setCreateTime(now);
            answer.setUpdateTime(now);

            if (question.getType() == QuestionTypeEnum.BriefResponse) {
                answer.setIsCorrect(null);
                answer.setScore(BigDecimal.ZERO);
                hasPendingSubjective = true;
            } else {
                boolean correct = isCorrect(question.getBody(), question.getType(), answer.getUserAnswer());
                BigDecimal questionScore = relation.getScore() == null ? BigDecimal.ZERO : relation.getScore();
                answer.setIsCorrect(correct);
                answer.setScore(correct ? questionScore : BigDecimal.ZERO);
                if (correct) {
                    totalScore = totalScore.add(questionScore);
                } else {
                    saveErrorBookIfAbsent(userId, examId, relation.getQuestionId(), now);
                }
            }
            answersToSave.add(answer);
        }

        userAnswerService.saveBatch(answersToSave);
        record.setTotalScore(totalScore);
        record.setStatus(hasPendingSubjective ? 1 : 2);
        record.setUpdateTime(LocalDateTime.now());
        updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean gradeEssay(Long answerId, BigDecimal score) {
        if (answerId == null || score == null || score.signum() < 0) {
            throw new IllegalArgumentException("答题ID或分数不合法");
        }

        UserAnswer answer = userAnswerService.getById(answerId);
        if (answer == null || answer.getRecordId() == null) {
            throw new IllegalArgumentException("答题记录不存在");
        }
        ExamRecord record = getById(answer.getRecordId());
        if (record == null) {
            throw new IllegalArgumentException("考试记录不存在");
        }
        Question question = questionService.getById(answer.getQuestionId());
        if (question == null || question.getType() != QuestionTypeEnum.BriefResponse) {
            throw new IllegalArgumentException("只能批改主观题");
        }

        ExamQuestionRelation relation = examQuestionRelationService.lambdaQuery()
                .eq(ExamQuestionRelation::getExamId, record.getExamId())
                .eq(ExamQuestionRelation::getQuestionId, answer.getQuestionId())
                .one();
        if (relation == null) {
            throw new IllegalArgumentException("题目不属于该考试");
        }
        BigDecimal maxScore = relation.getScore() == null ? BigDecimal.ZERO : relation.getScore();
        if (score.compareTo(maxScore) > 0) {
            throw new IllegalArgumentException("得分不能超过题目分值");
        }

        LocalDateTime now = LocalDateTime.now();
        answer.setScore(score);
        answer.setIsCorrect(score.signum() > 0);
        answer.setUpdateTime(now);
        userAnswerService.updateById(answer);

        List<UserAnswer> allAnswers = userAnswerService.lambdaQuery()
                .eq(UserAnswer::getRecordId, record.getRecordId())
                .list();
        BigDecimal totalScore = allAnswers.stream()
                .map(UserAnswer::getScore)
                .filter(scoreValue -> scoreValue != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean hasPendingSubjective = allAnswers.stream().anyMatch(item -> item.getIsCorrect() == null);
        record.setTotalScore(totalScore);
        record.setStatus(hasPendingSubjective ? 1 : 2);
        record.setUpdateTime(now);
        updateById(record);

        if (score.signum() == 0) {
            saveErrorBookIfAbsent(record.getUserId(), record.getExamId(), answer.getQuestionId(), now);
        }
        return true;
    }

    private void saveErrorBookIfAbsent(Long userId, Long examId, Long questionId, LocalDateTime now) {
        boolean exists = errorBookService.lambdaQuery()
                .eq(ErrorBook::getUserId, userId)
                .eq(ErrorBook::getExamId, examId)
                .eq(ErrorBook::getQuestionId, questionId)
                .exists();
        if (!exists) {
            ErrorBook errorBook = new ErrorBook();
            errorBook.setUserId(userId);
            errorBook.setQuestionId(questionId);
            errorBook.setExamId(examId);
            errorBook.setCreateTime(now);
            errorBook.setUpdateTime(now);
            errorBookService.save(errorBook);
        }
    }

    private boolean isCorrect(QuestionBody body, QuestionTypeEnum type, String userAnswer) {
        if (body == null || body.getCorrect() == null || userAnswer == null) {
            return false;
        }
        if (type == QuestionTypeEnum.MultiOption) {
            return toAnswerSet(body.getCorrect()).equals(toAnswerSet(userAnswer));
        }
        if (type == QuestionTypeEnum.Judge) {
            return normalizeJudge(body.getCorrect()).equals(normalizeJudge(userAnswer));
        }
        return normalizeScalar(body.getCorrect()).equalsIgnoreCase(normalizeScalar(userAnswer));
    }

    private String normalizeJudge(Object value) {
        String normalized = normalizeScalar(value);
        return switch (normalized) {
            case "正确", "对", "TRUE", "YES", "是", "T" -> "T";
            case "错误", "错", "FALSE", "NO", "否", "F" -> "F";
            default -> normalized;
        };
    }

    private Set<String> toAnswerSet(Object value) {
        Set<String> result = new HashSet<>();
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> addNormalizedToken(result, item));
            return result;
        }
        String text = normalizeScalar(value);
        if (text.startsWith("[") && text.endsWith("]")) {
            text = text.substring(1, text.length() - 1);
        }
        for (String item : text.split("[,，、\\s]+")) {
            addNormalizedToken(result, item);
        }
        return result;
    }

    private void addNormalizedToken(Set<String> target, Object value) {
        if (value != null) {
            String token = normalizeScalar(value);
            if (!token.isBlank()) {
                target.add(token);
            }
        }
    }

    private String normalizeScalar(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value)
                .trim()
                .replace("\"", "")
                .replace("'", "")
                .toUpperCase(Locale.ROOT);
    }
}
