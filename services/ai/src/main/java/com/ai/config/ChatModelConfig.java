package com.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientBuilderConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * description
 * ChatModel初始化后无法修改，因为ChatOption不可更改，即使拿到也是一个副本。这导致只能使用某一个模型进行交流，如千问默认的是G什么的。
 * 而ChatClient提供了mutate方法可以修改全局配置，也在prompt方法时提供临时修改配置的方法，所以用ChatClient更好。
 * author zzq
 * date 2025/12/15 15:07
 */
@Configuration
public class ChatModelConfig {

    /**
     * description 覆盖默认的 ChatClient.Builder 的bean配置。
     * author zzq
     * date 2025/12/15 12:56
     * param
     * return
     */
    @Bean
    @Scope("prototype")
    ChatClient.Builder ollamaChatClientBuilder(ChatClientBuilderConfigurer configurer,
                                         @Qualifier("ollamaChatModel") ChatModel ollamaChatModel,
                                         @Value("${OLLAMA_CHAT_MODEL:qwen3:4b}") String model,
                                         @Value("${OLLAMA_NUM_PREDICT:512}") Integer numPredict,
                                         @Value("${OLLAMA_NUM_CTX:8192}") Integer numCtx,
                                         @Value("${OLLAMA_TEMPERATURE:0.3}") Double temperature,
                                         @Value("${OLLAMA_THINK:false}") Boolean thinking){
        // 1. 手动创建一个绑定了 Ollama 的 Builder
        ChatClient.Builder builder = ChatClient.builder(ollamaChatModel);

        // 2. 让 Configurer 把其他的默认设置（如 Observation 等）应用上去
        // 这样既解决了冲突，又保留了 Spring AI 的其他自动配置特性
        ChatClient.Builder configuredBuilder = configurer.configure(builder);

        // ThinkOption 是 Spring AI 1.1 的接口类型，直接通过 YAML 的 think 字段绑定存在歧义。
        // 在统一 Builder 上显式设置，确保 qwen3 的思考内容不会被误当成空答案返回。
        OllamaChatOptions.Builder optionsBuilder = OllamaChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .numPredict(numPredict)
                .numCtx(numCtx);
        if (Boolean.TRUE.equals(thinking)) {
            optionsBuilder.enableThinking();
        } else {
            optionsBuilder.disableThinking();
        }
        return configuredBuilder.defaultOptions(optionsBuilder.build());
    }

}
