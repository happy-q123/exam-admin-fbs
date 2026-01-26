package com.ai.controller;//package com.test.multiagentbendiollamaqwen3vl4b.controller;
//
//import org.springframework.ai.chat.model.ChatModel;
//import org.springframework.ai.chat.model.ChatResponse;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import reactor.core.publisher.Flux;
//
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@RestController
//public class ExampleController {
//    private final ChatModel chatModel;
//    private final ChatMemoryServiceImpl chatMemoryService;
//    private final EmbeddingModel embeddingModel;
//
//    public ExampleController(@Qualifier("ollamaChatModel") ChatModel chatModel,
//                             ChatMemoryServiceImpl chatMemoryService, @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {
//        this.chatModel = chatModel;
//        this.chatMemoryService = chatMemoryService;
//        this.embeddingModel = embeddingModel;
//        String x=chatModel.call("你好");
//        System.out.println(x);
//    }
//    @GetMapping("/testBenDiModel")
//    public String testBenDiModel(@RequestParam("query") String query){
//        String x=chatModel.call(query);
//        return x;
//    }
//
//    @GetMapping("/memoryChat")
//    public String chatMemory(@RequestParam("query") String query){
//        String x=chatMemoryService.memoryChat(query,"111");
//        return x;
//    }
//
//    @GetMapping(value = "/streamMemoryChat",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<ChatResponse> streamMemoryChat(@RequestParam("query") String query){
//        return chatMemoryService.streamMemoryChat(query,"111");
//    }
//
//    @GetMapping("/embed")
//    public float [] embedText(@RequestParam String text) {
//        // 将文本转换为向量 (List<Double>)
//        return embeddingModel.embed(text);
//    }
//
//
//    @GetMapping("/search")
//    public List<Map<String, Object>> search(@RequestParam String query) {
//
//        List<Document> documents = chatMemoryService.searchKnowledgeBase(query);
//
//        // 为了前端展示更清晰，我们可以转换一下格式，把 metadata 和 content 分开
//        // 或者直接返回 List<Document> 也可以，Spring Boot 会自动转 JSON
//        return documents.stream().map(doc -> Map.of(
//                "id", doc.getId(),
//                "content", doc.getText(), // 文本内容
//                "metadata", doc.getMetadata() // 元数据（包含 source, book 等）
//                // 注意：Spring AI 目前的 Document 对象通常不直接包含 score (相似度分数)，
//                // 除非 VectorStore 实现将其放入了 metadata 中。
//        )).collect(Collectors.toList());
//    }
//}
