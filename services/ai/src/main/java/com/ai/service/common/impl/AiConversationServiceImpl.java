package com.ai.service.common.impl;

import com.ai.mapper.AiConversationMapper;
import com.ai.service.common.AiConversationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.AiConversation;
import org.springframework.stereotype.Service;

@Service
public class AiConversationServiceImpl extends ServiceImpl<AiConversationMapper, AiConversation>
        implements AiConversationService {
}
