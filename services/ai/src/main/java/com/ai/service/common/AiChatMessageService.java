package com.ai.service.common;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.dto.ChatMessageComposeDto;
import com.domain.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AiChatMessageService extends IService<ChatMessage> {

    /**
     * 语义搜索历史消息
     */
    List<ChatMessage> searchSimilarMessages(String query, int topK);


    /**
     * description 根据用户ID + 会话ID 进行向量相似度搜索
     * author zzq
     * date 2026/1/31 19:47
    */
    List<ChatMessageComposeDto> searchSimilarMessages(Long userId, Long conversationId, String query, int limit);

    /**
     * 保存问答对
     * 内部逻辑应包含：生成 userEmbedding 和 aiEmbedding，然后存入数据库。返回保存成功的ID
     */
    Long saveChatPair(String userContent, LocalDateTime userCreateTime, String aiContent, LocalDateTime aiCreateTime);

    /**
     * 按会话顺序读取完整历史。语义检索只用于补充上下文，不能替代用户可见的聊天记录。
     */
    List<ChatMessageComposeDto> findConversationMessages(Long userId, Long conversationId);
}
