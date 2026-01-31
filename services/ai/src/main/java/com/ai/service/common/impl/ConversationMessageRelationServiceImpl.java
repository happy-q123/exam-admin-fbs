package com.ai.service.common.impl;

import com.ai.mapper.ConversationMessageRelationMapper;
import com.ai.service.common.ConversationMessageRelationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.relation.ConversationMessageRelation;
import org.springframework.stereotype.Service;

@Service
public class ConversationMessageRelationServiceImpl
        extends ServiceImpl<ConversationMessageRelationMapper, ConversationMessageRelation>
        implements ConversationMessageRelationService {
}
