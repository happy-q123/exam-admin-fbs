package com.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domain.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import javax.management.MXBean;
import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 向量相似度搜索
     * * 原理：
     * 1. (embedding <=> vector) 计算余弦距离 (0=完全一样, 2=完全相反)
     * 2. 1 - distance 将距离转换为相似度 (1=完全一样, -1=完全相反)
     * 3. ORDER BY ... ASC LIMIT N 取最相似的前 N 条
     * * @param vectorJson 向量的字符串形式，例如 "[0.1, 0.2, 0.3 ...]"
     * @param limit 取前几条
     * @return 带有相似度分数的 ChatMessage 列表
     */
    @Select("SELECT id, message_content, role, created_at, " +
            "1 - (embedding <=> #{vectorJson}::vector) as similarity " + // 计算相似度
            "FROM chat_message " +
            "ORDER BY embedding <=> #{vectorJson}::vector ASC " + // 按距离排序
            "LIMIT #{limit}")
    List<ChatMessage> searchByVector(@Param("vectorJson") String vectorJson, @Param("limit") int limit);

}
