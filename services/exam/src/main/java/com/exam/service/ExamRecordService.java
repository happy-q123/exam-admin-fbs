package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.entity.ExamRecord;
import com.domain.entity.UserAnswer;
import java.util.List;

/**
 * 考试记录服务类
 *
 * @author zzq
 * @date 2026-06-11
 */
public interface ExamRecordService extends IService<ExamRecord> {

    /**
     * 提交试卷，实现自动判分逻辑并记录错题 (已改为异步非阻塞执行)
     */
    void submitExam(Long userId, Long examId, List<UserAnswer> userAnswers);

    /**
     * 教师手动批改主观题
     * @param answerId 答题明细ID
     * @param score 教师给定的分数
     * @return 是否批改成功
     */
    boolean gradeEssay(Long answerId, java.math.BigDecimal score);
}
