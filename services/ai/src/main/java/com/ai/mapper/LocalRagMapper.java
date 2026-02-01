package com.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domain.entity.LocalRag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LocalRagMapper extends BaseMapper<LocalRag> {
    /**
     * 纯向量相似度搜索
     * * @param vectorJson 向量字符串，格式如 "[0.123, -0.456, ...]"
     * @param limit      返回条数限制
     * @return 带有相似度分数的文档列表
     */
    @Select("SELECT id, rag_source, content, created_time, " +
            "1 - (embedding <=> #{vectorJson}::vector) as similarity " +
            "FROM local_rag " +
            "ORDER BY embedding <=> #{vectorJson}::vector ASC " +
            "LIMIT #{limit}")
    List<LocalRag> searchKnowledge(@Param("vectorJson") String vectorJson,
                                   @Param("limit") int limit);


//    /**
//     * 带过滤条件的向量搜索
//     *
//     * @param vectorJson 向量字符串
//     * @param source     数据来源 (如 "西游记")，传 null 则不过滤
//     * @param limit      返回条数
//     */
//    @Select("""
//    <script>
//    <![CDATA[
//    SELECT id, rag_source, content, created_time,
//           1 - (embedding <=> #{vectorJson}::vector) AS similarity
//    FROM local_rag
//    WHERE 1 = 1
//    ]]>
//    <if test="source != null and source != ''">
//        <![CDATA[
//        AND rag_source = #{source}
//        ]]>
//    </if>
//    <![CDATA[
//    ORDER BY embedding <=> #{vectorJson}::vector ASC
//    LIMIT #{limit}
//    ]]>
//    </script>
//    """)
//
//    List<LocalRag> searchKnowledgeWithFilter(@Param("vectorJson") String vectorJson,
//                                             @Param("source") String source,
//                                             @Param("limit") int limit);

    /**
     * 支持 ragName 为列表形式的过滤
     *
     * @param vectorJson 向量字符串
     * @param sources    数据来源列表 (List<String>)
     * @param limit      返回条数
     */
    @Select("""
    <script>
    <![CDATA[
    SELECT id, rag_source, content, created_time,
           1 - (embedding <=> #{vectorJson}::vector) AS similarity
    FROM local_rag
    WHERE 1 = 1
    ]]>
    
    <if test="sources != null and !sources.isEmpty()">
        AND rag_source IN
        <foreach collection="sources" item="item" open="(" separator="," close=")">
            #{item}
        </foreach>
    </if>
    
    <![CDATA[
    ORDER BY embedding <=> #{vectorJson}::vector ASC
    LIMIT #{limit}
    ]]>
    </script>
    """)
    List<LocalRag> searchKnowledgeWithFilter(@Param("vectorJson") String vectorJson,
                                             @Param("sources") List<String> sources,
                                             @Param("limit") int limit);
}
