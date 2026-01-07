package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domain.entity.relation.UserApplyExamRelation;
import com.domain.entity.relation.UserOnlineExamOptions;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserOnlineExamOptionsMapper extends BaseMapper<UserOnlineExamOptions> {
}
