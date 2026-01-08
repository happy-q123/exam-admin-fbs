package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.dto.ExamDto;
import com.domain.entity.Exam;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public interface ExamService extends IService<Exam> {

    /**
     * description 考试报名
     * author zzq
     * date 2025/12/18 15:28
     * param
     * return
     */
    boolean userApplyExam(Long userId, Long examId);

    /**
     * description 添加考试
     * author zzq
     * date 2025/12/19 15:33
     * param * @param null
     * return
     */
    boolean insert(ExamDto dto);

    /**
     * description 检查当前时间是否位于考试的结束时间之后。
     * 会查询redis，如果redis没有，则查询数据库，并写入redis
     * author zzq
     * date 2026/1/8 11:06
     */
    boolean currentIsByondExamExpireTime(Long examId, @Nullable LocalDateTime acquireTime);


    /**
     * description 获取考试最大进入次数
     * author zzq
     * date 2026/1/8 11:19
     */
    Long getEnterExamMaxCount(Long examId);


}
