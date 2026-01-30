package com.ai.service.common;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.entity.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface AiChatMessageService extends IService<ChatMessage> {

    /**
     * 语义搜索历史消息
     */
    List<ChatMessage> searchSimilarMessages(String query, int topK);

    /**
     * 保存问答对
     * 内部逻辑应包含：生成 userEmbedding 和 aiEmbedding，然后存入数据库
     */
    void saveChatPair(String userContent, LocalDateTime userCreateTime, String aiContent, LocalDateTime aiCreateTime);
}
