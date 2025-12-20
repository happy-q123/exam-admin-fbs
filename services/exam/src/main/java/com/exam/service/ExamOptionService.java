package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.dto.ExamDto;
import com.domain.entity.Exam;

public interface ExamOptionService extends IService<Exam> {

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

}
