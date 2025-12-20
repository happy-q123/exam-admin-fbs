package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domain.entity.relation.ExamQuestionRelation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamQuestionRelationMapper extends BaseMapper<ExamQuestionRelation> {
}
