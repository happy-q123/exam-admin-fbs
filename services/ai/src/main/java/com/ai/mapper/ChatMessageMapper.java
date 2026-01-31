package com.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domain.dto.ChatMessageComposeDto;
import com.domain.entity.ChatMessage;
import com.domain.handler.PostgreSQLVectorTypeHandler;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.type.JdbcType;

import javax.management.MXBean;
import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 向量相似度搜索
     * * 原理：
     * 1. (user_embedding <=> vector) 计算余弦距离 (0=完全一样, 2=完全相反)
     * 2. 1 - distance 将距离转换为相似度 (1=完全一样, -1=完全相反)
     * 3. ORDER BY ... ASC LIMIT N 取最相似的前 N 条
     * * @param vectorJson 向量的字符串形式，例如 "[0.1, 0.2, 0.3 ...]"
     * @param limit 取前几条
     * @return 带有相似度分数的 ChatMessage 列表
     */
    @Select("SELECT id, user_content, ai_content, user_created_time,ai_created_time, " +
            "1 - (user_embedding <=> #{vectorJson}::vector) as similarity " + // 计算相似度
            "FROM chat_message " +
            "ORDER BY user_embedding <=> #{vectorJson}::vector ASC " + // 按距离排序
            "LIMIT #{limit}")
    List<ChatMessage> searchByVector(@Param("vectorJson") String vectorJson, @Param("limit") int limit);

    /**
     * 根据 用户ID + 会话ID 进行向量相似度搜索
     *
     * 逻辑：
     * 1. 联表 chat_message -> conversation_message_relation (获取会话关联)
     * 2. 联表 -> user_conversation_relation (获取用户关联，用于鉴权和过滤)
     * 3. 计算 user_embedding 与输入向量的余弦距离
     * 4. 排序并分页
     */
    @Select("SELECT " +
            "  m.id AS message_id, " +
            "  r1.conversation_id, " +
            "  r2.user_id, " +
            "  m.user_content, " +
            "  m.user_embedding, " +
            "  m.user_created_time, " +
            "  m.ai_content, " +
            "  m.ai_embedding, " +
            "  m.ai_created_time, " +
            "  1 - (m.user_embedding <=> #{vectorJson}::vector) as similarity " +
            "FROM chat_message m " +
            // 关联表1：消息 -> 会话
            "INNER JOIN conversation_message_relation r1 ON m.id = r1.message_id " +
            // 关联表2：会话 -> 用户 (用于确保该会话确实属于该用户)
            "INNER JOIN user_conversation_relation r2 ON r1.conversation_id = r2.conversation_id " +
            "WHERE " +
            "  r2.user_id = #{userId} " +
            "  AND r1.conversation_id = #{conversationId} " +
            "ORDER BY m.user_embedding <=> #{vectorJson}::vector ASC " +
            "LIMIT #{limit}")
    @Results(id = "ChatMessageDtoMap", value = {
            // 基础字段映射
            @Result(property = "messageId", column = "message_id"),
            @Result(property = "conversationId", column = "conversation_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "userContent", column = "user_content"),
            @Result(property = "userCreatedTime", column = "user_created_time"),
            @Result(property = "aiContent", column = "ai_content"),
            @Result(property = "aiCreatedTime", column = "ai_created_time"),
            @Result(property = "similarity", column = "similarity"),

            // --- 重点：向量字段必须显式指定 TypeHandler ---
            @Result(property = "userEmbedding", column = "user_embedding",
                    typeHandler = PostgreSQLVectorTypeHandler.class, javaType = List.class, jdbcType = JdbcType.OTHER),
            @Result(property = "aiEmbedding", column = "ai_embedding",
                    typeHandler = PostgreSQLVectorTypeHandler.class, javaType = List.class, jdbcType = JdbcType.OTHER)
    })
    List<ChatMessageComposeDto> searchVectorInConversation(
            @Param("userId") Long userId,
            @Param("conversationId") Long conversationId,
            @Param("vectorJson") String vectorJson,
            @Param("limit") int limit
    );
}
