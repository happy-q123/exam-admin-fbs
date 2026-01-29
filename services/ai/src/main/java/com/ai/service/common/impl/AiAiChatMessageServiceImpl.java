package com.ai.service.common.impl;

import com.ai.mapper.ChatMessageMapper;
import com.ai.service.common.AiChatMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.ChatMessage;
import org.springframework.stereotype.Service;

@Service
public class AiAiChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
        implements AiChatMessageService {

}
