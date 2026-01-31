package com.ai.service.common.impl;

import com.ai.mapper.ChatMessageMapper;
import com.ai.service.common.AiChatMessageService;
import com.ai.utils.EmbedOptionsUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.dto.ChatMessageComposeDto;
import com.domain.entity.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
        implements AiChatMessageService {


    @Resource
    private OllamaEmbeddingModel embeddingModel;

    private String queryToJson(String query){
        float[] fv= embeddingModel.embed(query);
        // 将 float 数组转换为 List<Double>
        List<Double> vector = EmbedOptionsUtil.floatArrayToDoubleList(fv);

        // 将 List<Double> 转换为 JSON 字符串
        ObjectMapper objectMapper = new ObjectMapper();
        String vectorStr;
        try {
            vectorStr = objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            throw new RuntimeException("向量序列化失败", e);
        }
        return vectorStr;
    }

    @Override
    public List<ChatMessage> searchSimilarMessages(String query, int topK) {
        String vectorStr = this.queryToJson(query);
        return getBaseMapper().searchByVector(vectorStr, topK);
    }

    @Override
    public List<ChatMessageComposeDto> searchSimilarMessages(Long userId, Long conversationId, String query, int limit) {
        String vectorStr = this.queryToJson(query);
        getBaseMapper().searchVectorInConversation(userId, conversationId, vectorStr, limit);
        return getBaseMapper().searchVectorInConversation(userId, conversationId, vectorStr, limit);
    }

    @Async // 建议异步保存，不阻塞 AI 回复用户的速度
    @Override
    public Long saveChatPair(String userContent, LocalDateTime userCreateTime,
                             String aiContent, LocalDateTime aiCreateTime) {
        // 1. 生成两个向量
        float[] fv1 = embeddingModel.embed(userContent);
        List<Double> userVec = EmbedOptionsUtil.floatArrayToDoubleList(fv1);
        float[] fv2 = embeddingModel.embed(aiContent);
        List<Double> aiVec = EmbedOptionsUtil.floatArrayToDoubleList(fv2);

        // 2. 存库
        ChatMessage msg = new ChatMessage();
        msg.setUserContent(userContent)
                .setUserEmbedding(userVec)
                .setAiContent(aiContent)
                .setAiEmbedding(aiVec)
                .setUserCreatedTime(userCreateTime)
                .setAiCreatedTime(aiCreateTime);

        // msg.setMessageSource(messageSource); // 如果表里有这个字段
        save(msg);
        return msg.getId();
    }
}
