package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domain.entity.UserAnswer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户答题明细 Mapper 接口
 *
 * @author zzq
 * @date 2026-06-11
 */
@Mapper
public interface UserAnswerMapper extends BaseMapper<UserAnswer> {
}
