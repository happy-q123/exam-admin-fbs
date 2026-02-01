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

    @Value("${redis-stack.password:123456}")
    private String password;
//    RedisVectorStore

    @Bean
    public RedisVectorStore messageVectorStore(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel, JedisConnectionFactory jedisConnectionFactory) {
        // --- 1. 像官方一样构建配置，支持 SSL 和 超时 ---
        redis.clients.jedis.DefaultJedisClientConfig.Builder configBuilder =
                redis.clients.jedis.DefaultJedisClientConfig.builder();
        // 设置 SSL
        if (jedisConnectionFactory.isUseSsl()) {
            configBuilder.ssl(true);
        }
        // 这里我们要优先使用你自己定义的 password 变量
        // 因为你的端口改成了 this.redisStackPort，密码也应该配套使用 this.password
        if (this.password != null && !this.password.isEmpty()) {
            configBuilder.password(this.password);
        } else if (jedisConnectionFactory.getPassword() != null) {
            // 如果自定义密码为空，再尝试兜底使用全局 Redis 密码
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
        // 保存的元数据是可以自己定义的，但是搜索时取出的元数据就要在下面定义。
        // 即保存时，我可能保存了字段w，但取出时取出的元数据可能没有字段w，如果想要w，需要在下面添加
        List<RedisVectorStore.MetadataField> metadataFields = List.of(
                RedisVectorStore.MetadataField.tag("conversationId"),
                RedisVectorStore.MetadataField.tag("userId"),
                RedisVectorStore.MetadataField.tag("userCreatedTime")
        );

        // 3. 构建 Store
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("exam-fbs-message")
                .metadataFields(metadataFields)
                .initializeSchema(true)
                .build();
    }

    //抄官方版的下面
    @Bean
    public RedisVectorStore ragVectorStore(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel, JedisConnectionFactory jedisConnectionFactory) {
        // --- 1. 像官方一样构建配置，支持 SSL 和 超时 ---
        redis.clients.jedis.DefaultJedisClientConfig.Builder configBuilder =
                redis.clients.jedis.DefaultJedisClientConfig.builder();
        // 设置 SSL
        if (jedisConnectionFactory.isUseSsl()) {
            configBuilder.ssl(true);
        }
        // 这里我们要优先使用你自己定义的 password 变量
        // 因为你的端口改成了 this.redisStackPort，密码也应该配套使用 this.password
        if (this.password != null && !this.password.isEmpty()) {
            configBuilder.password(this.password);
        } else if (jedisConnectionFactory.getPassword() != null) {
            // 如果自定义密码为空，再尝试兜底使用全局 Redis 密码
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
                RedisVectorStore.MetadataField.tag("createdTime"),
                RedisVectorStore.MetadataField.tag("ragId"),
                RedisVectorStore.MetadataField.tag("messageSource")//
        );

        // 3. 构建 Store
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName("exam-fbs-rag")
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
