package com.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domain.entity.ErrorBook;
import org.apache.ibatis.annotations.Mapper;

/**
 * 错题本 Mapper 接口
 *
 * @author zzq
 * @date 2026-06-11
 */
@Mapper
public interface ErrorBookMapper extends BaseMapper<ErrorBook> {
}
