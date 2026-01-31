package com.ai.service.common.impl;

import com.ai.mapper.UserConversationRelationMapper;
import com.ai.service.common.UserConversationRelationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.relation.UserConversationRelation;
import org.springframework.stereotype.Service;

@Service
public class UserConversationRelationServiceImpl
        extends ServiceImpl<UserConversationRelationMapper, UserConversationRelation>
        implements UserConversationRelationService {



}
