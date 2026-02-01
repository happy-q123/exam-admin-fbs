package com.ai.test;

import com.ai.mapper.LocalRagMapper;
import com.domain.entity.LocalRag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest
public class JourneyToWestRagTest {

    @Resource
    private LocalRagMapper localRagMapper;

    @Resource
    private OllamaEmbeddingModel embeddingModel; // Spring AI 自动注入的模型

    @Value("classpath:西游记utf8.txt")
    private org.springframework.core.io.Resource journeyRes;

    /**
     * 第一步：构建知识库
     * 读取西游记文本 -> 切片 -> 向量化 -> 存入 PostgreSQL
     */
    @Test
    public void buildJourneyToWestKnowledgeBase() throws IOException {
        // 1. 读取文件
        if (!journeyRes.exists()) {
            log.error("❌ 未找到《西游记》文件，请检查 resources 目录");
            return;
        }
        
        // 读取全部文本
        String fullText = new String(journeyRes.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        log.info("📚 读入文本长度: {} 字", fullText.length());

        // 2. 智能切分 (复用你提供的逻辑)
        // 建议切分长度：300-500字，既能包含上下文，又不会超出一般 Embedding 模型的 token 限制
        List<String> chunks = smartSplitIntoChunks(fullText, 400);
        log.info("✂️ 切分为 {} 个片段，开始向量化并入库...", chunks.size());

        // 3. 循环处理并入库
        int successCount = 0;
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            
            // 跳过过短的无意义片段
            if (chunkText.length() < 10) continue;

            try {
                // A. 调用大模型生成向量 (这是最耗时的步骤)
                // Spring AI 的 embeddingModel.embed() 返回 float[]
                float[] r = embeddingModel.embed(chunkText);
                // 将float数组转换为List<Double>
                List<Double> vector = new ArrayList<>();
                for (float v : r) {
                    vector.add((double) v);
                }

                // B. 构建实体类
                LocalRag rag = new LocalRag();
                rag.setRagSource("西游记"); // 对应字段 messageSource
                rag.setContent(chunkText);      // 对应字段 content
                rag.setEmbedding(vector);       // 对应字段 embedding
                rag.setCreatedTime(LocalDateTime.now()); // 注意：使用 createdTime

                // C. 插入数据库
                localRagMapper.insert(rag);
                
                successCount++;
                if (successCount % 10 == 0) {
                    log.info("⏳ 已处理 {} / {} 条...", successCount, chunks.size());
                }

            } catch (Exception e) {
                log.error("❌ 片段 [{}] 处理失败: {}", i, e.getMessage());
            }
        }

        log.info("✅ 《西游记》RAG 构建完成！共入库 {} 条数据。", successCount);
    }

    /**
     * 第二步：测试检索
     * 验证数据是否真的进去了，并且能搜出来
     */
    @Test
    public void testSearchJourney() {
        // 0. 定义问题
        String query = "孙悟空是在哪里学会的长生不老术？";
        log.info("❓ [Step 1] 用户提问: {}", query);

        // 1. 将问题转化为向量
        // Spring AI 的 embeddingModel.embed(String) 默认返回 List<Double>，
        // 如果你的版本返回 float[]，请保留你之前的转换逻辑，但通常不需要。
        float[] r = embeddingModel.embed(query);
        List<Double> queryVector = new ArrayList<>();
        for (float v : r) {
            queryVector.add((double) v);
        }


        // 2. 转换为 PGvector 需要的字符串格式 "[0.123, 0.456, ...]"
        // ArrayList.toString() 生成的格式正好符合 PGvector 要求
        String vectorJson = queryVector.toString();

        log.info("🤖 [Step 2] 向量生成完毕，维度: {}, 正在查询数据库...", queryVector.size());

        // 3. 调用 Mapper 执行向量检索 (取最相似的前 3 条)
        List<LocalRag> results = localRagMapper.searchKnowledge(vectorJson, 3);

        // 4. 打印并验证结果
        if (results.isEmpty()) {
            log.warn("⚠️ 未找到相关内容，请检查数据库是否已导入数据。");
            return;
        }

        log.info("🔍 [Step 3] 检索成功，找到 {} 条相关记录：", results.size());

        for (int i = 0; i < results.size(); i++) {
            LocalRag rag = results.get(i);
            // 格式化输出，方便控制台查看
            log.info("""
                
                🏆 第 {} 名 (相似度: {})
                📚 来源: {}
                📝 内容: {}
                --------------------------------------------------
                """,
                    i + 1,
                    String.format("%.4f", rag.getSimilarity()), // 保留4位小数
                    rag.getRagSource(),
                    rag.getContent()
            );
        }
    }
    
    // ================== 工具方法 (从 Code 1 移植并适配) ==================

    /**
     * 将 List<Double> 转为 PostgreSQL 向量字符串格式 "[0.1, 0.2, ...]"
     */
    private String listToString(List<Double> list) {
        return list.toString();
    }

    /**
     * 智能文本切分逻辑
     * (直接复用 Code 1 的逻辑，为了保持测试类独立，我复制过来了)
     */
    private List<String> smartSplitIntoChunks(String text, int maxChars) {
        List<String> chunks = new ArrayList<>();
        // 简单按双换行分段，防止切断段落
        String[] paragraphs = text.split("\n\n"); 
        StringBuilder currentChunk = new StringBuilder();

        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;

            if (para.length() > maxChars) {
                // 如果单段本身就超长，强制按句号切分
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }
                String[] sentences = para.split("(?<=[。？！])");
                for (String sentence : sentences) {
                    if (currentChunk.length() + sentence.length() <= maxChars) {
                        currentChunk.append(sentence);
                    } else {
                        if (currentChunk.length() > 0) chunks.add(currentChunk.toString());
                        currentChunk = new StringBuilder(sentence);
                    }
                }
            } else {
                // 拼接段落
                if (currentChunk.length() + para.length() <= maxChars) {
                    if (currentChunk.length() > 0) currentChunk.append("\n\n");
                    currentChunk.append(para);
                } else {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder(para);
                }
            }
        }
        if (currentChunk.length() > 0) chunks.add(currentChunk.toString());
        return chunks;
    }
}