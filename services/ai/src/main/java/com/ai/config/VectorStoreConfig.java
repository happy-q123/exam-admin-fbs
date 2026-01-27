package com.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.JedisPooled;

import java.util.List;
@Configuration
public class VectorStoreConfig {

    @Value("${redis-stack.port:6378}") // 冒号后是默认值，如果配置文件没写就用6378
    private int redisStackPort;
//    RedisVectorStore
    //抄官方版的下面
    @Bean
    public RedisVectorStore vectorStore(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel, JedisConnectionFactory jedisConnectionFactory) {
        // --- 1. 像官方一样构建配置，支持 SSL 和 超时 ---
        redis.clients.jedis.DefaultJedisClientConfig.Builder configBuilder =
                redis.clients.jedis.DefaultJedisClientConfig.builder();
        // 设置 SSL
        if (jedisConnectionFactory.isUseSsl()) {
            configBuilder.ssl(true);
        }
        // 设置密码
        if (jedisConnectionFactory.getPassword() != null) {
            configBuilder.password(jedisConnectionFactory.getPassword());
        }
        // 设置超时
        configBuilder.timeoutMillis(jedisConnectionFactory.getTimeout());
        // 设置 ClientName
        configBuilder.clientName(jedisConnectionFactory.getClientName());

        // 创建 JedisPooled (使用 HostAndPort + Config)
        JedisPooled jedisPooled = new JedisPooled(
//                new redis.clients.jedis.HostAndPort(jedisConnectionFactory.getHostName(), jedisConnectionFactory.getPort()),
                new redis.clients.jedis.HostAndPort(jedisConnectionFactory.getHostName(), this.redisStackPort),
                configBuilder.build()
        );

        // 2. 你的自定义 Metadata 逻辑
        List<RedisVectorStore.MetadataField> metadataFields = List.of(
                RedisVectorStore.MetadataField.tag("conversationId"),
                RedisVectorStore.MetadataField.tag("messageType"),
                RedisVectorStore.MetadataField.tag("userId"),
                //conversation or knowledge
                //todo
                // 自建库有这个tag值为knowledge，VectorStoreAdvisor类的tag为conversationId。
                // 因此这俩不干扰，目前正常运行。
                // 但是VectorStoreAdvisor类记录历史对话时添加的tag已经写死conversationId。
                // 而我的愿景是仅使用messageSource进行区分。
                RedisVectorStore.MetadataField.tag("messageSource")//
        );

        // 3. 构建 Store
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("llc")
                .metadataFields(metadataFields)
                .initializeSchema(true)
                .build();
    }


    //RedisVectorStore不支持多个索引，但可以通过创建多个RedisVectorStore实例来实现，如下面的实例就是索引为index2的RedisVectorStore
//    @Bean
//    public RedisVectorStore vectorStoreIndex2(EmbeddingModel embeddingModel, JedisConnectionFactory jedisConnectionFactory) {
//        // --- 1. 抄作业：像官方一样构建配置，支持 SSL 和 超时 ---
//        redis.clients.jedis.DefaultJedisClientConfig.Builder configBuilder =
//                redis.clients.jedis.DefaultJedisClientConfig.builder();
//
//        // 设置 SSL
//        if (jedisConnectionFactory.isUseSsl()) {
//            configBuilder.ssl(true);
//        }
//        // 设置密码
//        if (jedisConnectionFactory.getPassword() != null) {
//            configBuilder.password(jedisConnectionFactory.getPassword());
//        }
//        // 设置超时
//        configBuilder.timeoutMillis(jedisConnectionFactory.getTimeout());
//        // 设置 ClientName
//        configBuilder.clientName(jedisConnectionFactory.getClientName());
//
//        // 创建 JedisPooled (使用 HostAndPort + Config)
//        JedisPooled jedisPooled = new JedisPooled(
//                new redis.clients.jedis.HostAndPort(jedisConnectionFactory.getHostName(), jedisConnectionFactory.getPort()),
//                configBuilder.build()
//        );
//
//        // 2. 你的自定义 Metadata 逻辑
//        List<RedisVectorStore.MetadataField> metadataFields = List.of(
//                RedisVectorStore.MetadataField.tag("conversationId"),
//                RedisVectorStore.MetadataField.tag("messageType")
//        );
//
//        // 3. 构建 Store
//        return RedisVectorStore.builder(jedisPooled, embeddingModel)
//                .indexName("index2")
//                .metadataFields(metadataFields)
//                .initializeSchema(true)
//                .build();
//    }
}
