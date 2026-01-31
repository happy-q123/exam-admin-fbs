package com.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domain.entity.relation.ConversationMessageRelation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMessageRelationMapper extends BaseMapper<ConversationMessageRelation> {
}
