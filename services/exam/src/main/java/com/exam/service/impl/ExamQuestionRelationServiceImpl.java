package com.exam.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.relation.ExamQuestionRelation;
import com.exam.mapper.ExamQuestionRelationMapper;
import com.exam.service.ExamQuestionRelationService;
import org.springframework.stereotype.Service;

@Service
public class ExamQuestionRelationServiceImpl extends ServiceImpl<ExamQuestionRelationMapper, ExamQuestionRelation>
        implements ExamQuestionRelationService {

}
