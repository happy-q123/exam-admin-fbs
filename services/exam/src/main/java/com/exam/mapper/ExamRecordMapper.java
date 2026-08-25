package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domain.entity.ExamRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试记录 Mapper 接口
 *
 * @author zzq
 * @date 2026-06-11
 */
@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {
}
