//package com.ai.service.agent;
//
//import jakarta.annotation.PostConstruct;
//import org.apache.commons.csv.CSVFormat;
//import org.apache.commons.csv.CSVParser;
//import org.apache.commons.csv.CSVRecord;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.vectorstore.redis.RedisVectorStore;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.io.Resource;
//import org.springframework.stereotype.Service;
//import org.springframework.util.StringUtils;
//
//import java.io.InputStreamReader;
//import java.io.Reader;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
///**
// * 向量数据库加载服务
// * 专门负责将本地资源（CSV, TXT）处理并加载到 Redis 向量库中
// * 此类用来测试，作为例子
// */
//@Service
//public class VectorStoreLoaderService {
//
//    private final RedisVectorStore vectorStore;
//
//    // 资源路径配置
//    @Value("classpath:QAFull.csv")
//    private Resource csvResource;
//
//    @Value("classpath:西游记utf8.txt")
//    private Resource journeyToWestResource;
//
//    public VectorStoreLoaderService(RedisVectorStore vectorStore) {
//        this.vectorStore = vectorStore;
//    }
//
//    /**
//     * 初始化加载数据
//     * 如果需要在项目启动时自动加载，请取消注释 @PostConstruct 下的代码
//     */
//    @PostConstruct
//    public void init() {
//        // 这里控制开关，防止每次重启都重复加载（虽然做了幂等处理，但耗时）
//        // System.out.println("🔄 检测到启动，开始检查并加载知识库...");
//        // loadCsvToVectorStore();
////         loadJourneyToWestToVectorStore();
//    }
//
//    /**
//     * 公开方法：加载 CSV 数据
//     */
//    public void loadCsvToVectorStore() {
//        if (!checkResource(csvResource, "QAFull.csv")) return;
//
//        List<Document> documents = new ArrayList<>();
//
//        try (Reader reader = new InputStreamReader(csvResource.getInputStream(), StandardCharsets.UTF_8);
//             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
//                     .setHeader()
//                     .setSkipHeaderRecord(true)
//                     .setIgnoreHeaderCase(true)
//                     .setTrim(true)
//                     .setIgnoreEmptyLines(true)
//                     .build())) {
//
//            System.out.println("🔍 [Loader] 开始解析 CSV 文件...");
//
//            for (CSVRecord csvRecord : csvParser) {
//                String question = getSafeValue(csvRecord, "问题");
//                String answer = getSafeValue(csvRecord, "回答");
//
//                if (!StringUtils.hasText(question) || !StringUtils.hasText(answer)) {
//                    continue;
//                }
//
//                String content = question + "\n" + answer;
//
//                Map<String, Object> metadata = Map.of(
//                        "origin_question", question,
//                        "origin_answer", answer,
//                        "source", "csv_import",
//                        "messageSource", "knowledge" // 关键 tag，用于后续检索过滤
//                );
//
//                String id = UUID.nameUUIDFromBytes(content.getBytes(StandardCharsets.UTF_8)).toString();
//                documents.add(new Document(id, content, metadata));
//            }
//
//            if (!documents.isEmpty()) {
//                System.out.println("🚀 [Loader] 正在写入 " + documents.size() + " 条 CSV 数据到 Redis...");
//                vectorStore.add(documents);
//                System.out.println("✅ [Loader] CSV 数据加载完成。");
//            }
//
//        } catch (Exception e) {
//            System.err.println("❌ [Loader] CSV 加载失败");
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 公开方法：加载《西游记》文本
//     */
//    public void loadJourneyToWestToVectorStore() {
//        if (!checkResource(journeyToWestResource, "西游记utf8.txt")) return;
//
//        try {
//            String fullText = new String(journeyToWestResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
//            List<String> chunks = smartSplitIntoChunks(fullText, 300);
//
//            System.out.println("📚 [Loader] 《西游记》切分为 " + chunks.size() + " 个块");
//
//            List<Document> documents = new ArrayList<>();
//            for (int i = 0; i < chunks.size(); i++) {
//                String chunk = chunks.get(i).trim();
//                if (chunk.isEmpty()) continue;
//
//                Map<String, Object> metadata = Map.of(
//                        "source", "西游记utf8.txt",
//                        "chunk_index", i,
//                        "book", "西游记",
//                        "messageSource", "knowledge"
//                );
//
//                String id = UUID.nameUUIDFromBytes(chunk.getBytes(StandardCharsets.UTF_8)).toString();
//                documents.add(new Document(id, chunk, metadata));
//            }
//
//            if (!documents.isEmpty()) {
//                System.out.println("🚀 [Loader] 正在写入文本数据到 Redis...");
//                vectorStore.add(documents);
//                System.out.println("✅ [Loader] 《西游记》加载完成。");
//            }
//
//        } catch (Exception e) {
//            System.err.println("❌ [Loader] 文本加载失败");
//            e.printStackTrace();
//        }
//    }
//
//    // --- 以下是私有辅助工具方法 ---
//
//    private boolean checkResource(Resource resource, String name) {
//        if (resource == null || !resource.exists()) {
//            System.err.println("⚠️ [Loader] 找不到资源文件: " + name);
//            return false;
//        }
//        return true;
//    }
//
//    private List<String> smartSplitIntoChunks(String text, int maxChars) {
//        List<String> chunks = new ArrayList<>();
//        String[] paragraphs = text.split("\n\n");
//        StringBuilder currentChunk = new StringBuilder();
//
//        for (String para : paragraphs) {
//            para = para.trim();
//            if (para.isEmpty()) continue;
//
//            if (para.length() > maxChars) {
//                if (currentChunk.length() > 0) {
//                    chunks.add(currentChunk.toString());
//                    currentChunk = new StringBuilder();
//                }
//                String[] sentences = para.split("(?<=[。？！])");
//                for (String sentence : sentences) {
//                    if (currentChunk.length() + sentence.length() <= maxChars) {
//                        currentChunk.append(sentence);
//                    } else {
//                        if (currentChunk.length() > 0) chunks.add(currentChunk.toString());
//                        currentChunk = new StringBuilder(sentence);
//                    }
//                }
//            } else {
//                if (currentChunk.length() + para.length() <= maxChars) {
//                    if (currentChunk.length() > 0) currentChunk.append("\n\n");
//                    currentChunk.append(para);
//                } else {
//                    chunks.add(currentChunk.toString());
//                    currentChunk = new StringBuilder(para);
//                }
//            }
//        }
//        if (currentChunk.length() > 0) chunks.add(currentChunk.toString());
//        return chunks;
//    }
//
//    private String getSafeValue(CSVRecord record, String targetHeader) {
//        if (record.isMapped(targetHeader)) return record.get(targetHeader);
//        Map<String, Integer> headerMap = record.getParser().getHeaderMap();
//        for (String actualHeader : headerMap.keySet()) {
//            String cleanHeader = actualHeader.replaceAll("[\\p{Cf}\\s]", "");
//            if (cleanHeader.contains(targetHeader) || targetHeader.contains(cleanHeader)) {
//                return record.get(actualHeader);
//            }
//        }
//        return null;
//    }
//}