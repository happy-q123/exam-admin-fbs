package com.ai.service.common;

import com.domain.dto.ChatMessageComposeDto;
import com.domain.entity.ChatMessage;
import com.domain.entity.LocalRag;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * description 综合服务类，在该类编写需要多个chat服务组合的逻辑
 * author zzq
 * date 2026/1/31 20:49
*/
public interface AiChatComposeService {
    /**
     * description 创建一个新的会话。
     * author zzq
     * date 2026/1/31 20:49
    */
    Long createConversation(Long userId);

    /**
     * description 创建一个新的消息。返回消息ID
     * author zzq
     * date 2026/1/31 21:08
     */
    Long createMessage(Long conversationId,String userContent, LocalDateTime userCreateTime,
                       String aiContent, LocalDateTime aiCreateTime);

    /**
     * description 包装下而已
     * author zzq
     * date 2026/1/31 19:47
     */
    List<ChatMessageComposeDto> searchSimilarMessages(Long userId, Long conversationId, String query, int limit);

    /**
     * description 包装下而已
     * author zzq
     * date 2026/2/1 16:37
    */
    List<LocalRag> searchSimilarLocalRag(String query,@Nullable List<String> sources,  @Nullable int limit);
}
