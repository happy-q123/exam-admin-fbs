package com.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientBuilderConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
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
                                         @Qualifier("ollamaChatModel") ChatModel ollamaChatModel){
        // 1. 手动创建一个绑定了 Ollama 的 Builder
        ChatClient.Builder builder = ChatClient.builder(ollamaChatModel);

        // 2. 让 Configurer 把其他的默认设置（如 Observation 等）应用上去
        // 这样既解决了冲突，又保留了 Spring AI 的其他自动配置特性
        return configurer.configure(builder);
    }


    /**
     * description zhiPuChatClient
     * author zzq
     * date 2025/12/15 15:10
     * param
     * return
     */
    @Bean
    @Scope("prototype")
    ChatClient.Builder zhiPuChatClientBuilder(ChatClientBuilderConfigurer configurer,
                                              @Qualifier("zhiPuAiChatModel") ChatModel zhiPuAiChatModel){
        // 1. 手动创建一个绑定了 Ollama 的 Builder
        ChatClient.Builder builder = ChatClient.builder(zhiPuAiChatModel);
        // 2. 让 Configurer 把其他的默认设置（如 Observation 等）应用上去
        // 这样既解决了冲突，又保留了 Spring AI 的其他自动配置特性
        return configurer.configure(builder);
    }

}
