package com.exam.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.dto.ExamQuestionRelationDto;
import com.domain.entity.relation.ExamQuestionRelation;
import com.domain.vo.ExamQuestionRelationVo;

public interface ExamQuestionRelationService extends IService<ExamQuestionRelation> {
    /**
     * description 根据某个考试id，分页获取其所有题目
     * author zzq
     * date 2025/12/20 16:20
     * param * @param null
     * return
     */
     Page<ExamQuestionRelationVo> getExamQuestionsByExamId(ExamQuestionRelationDto dto);

}
