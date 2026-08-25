package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domain.entity.Question;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题库 Mapper
 * @author zzq
 * @date 2026-06-11
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
