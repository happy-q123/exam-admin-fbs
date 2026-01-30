package com.ai.controller;//package com.test.multiagentbendiollamaqwen3vl4b.controller;
import com.ai.service.agent.ChatService;
import com.ai.service.agent.impl.HybridCacheMemoryChatAgent;
import com.domain.restful.RestResponse;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiServiceController {
    private final ChatService chatService;
    private final HybridCacheMemoryChatAgent hybridCacheMemoryChatAgent;
    public AiServiceController(ChatService chatService, HybridCacheMemoryChatAgent hybridCacheMemoryChatAgent) {
        this.chatService = chatService;
        this.hybridCacheMemoryChatAgent = hybridCacheMemoryChatAgent;
    }

    @GetMapping("/memoryChat")
    public RestResponse testBenDiModel(@AuthenticationPrincipal Jwt jwt, @RequestParam("query") String query){
        Long userId = jwt.getClaim("userId");
        if (userId == null) {
            return RestResponse.fail("token中无userId");
        }
        ChatClientResponse chatClientResponse=(ChatClientResponse) chatService.memoryChatFlow(userId,query);
        return RestResponse.success(chatClientResponse.chatResponse().getResult().getOutput().getText());
    }

    @GetMapping("/hybridMemoryChatTest")
    public RestResponse hybridMemoryChatTest(@AuthenticationPrincipal Jwt jwt, @RequestParam("query") String query){
        Long userId = jwt.getClaim("userId");
        if (userId == null) {
            return RestResponse.fail("token中无userId");
        }
        ChatClientResponse chatClientResponse=
                (ChatClientResponse) hybridCacheMemoryChatAgent.execute(query,String.valueOf(userId));
        return RestResponse.success(chatClientResponse.chatResponse().getResult().getOutput().getText());
    }

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
}
