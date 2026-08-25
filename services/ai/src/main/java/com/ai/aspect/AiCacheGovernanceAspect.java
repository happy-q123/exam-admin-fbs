package com.ai.aspect;

import com.domain.annotation.CacheGoverned;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

/**
 * AI 计算缓存治理：缓存不可用时自动回源，避免缓存故障放大成业务故障。
 */
@Aspect
@Component
public class AiCacheGovernanceAspect {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AiCacheGovernanceAspect(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(cacheGoverned)")
    public Object cache(ProceedingJoinPoint joinPoint, CacheGoverned cacheGoverned) throws Throwable {
        String key = "ai:governed:" + cacheGoverned.namespace() + ":" + digest(joinPoint.getArgs());
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null && !cached.isBlank()) {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                JavaType type = objectMapper.getTypeFactory().constructType(signature.getMethod().getGenericReturnType());
                return objectMapper.readValue(cached, type);
            }
        } catch (Exception ignored) {
            // 读取缓存失败时继续回源。
        }

        Object result = joinPoint.proceed();
        if (result != null) {
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result),
                        Duration.ofSeconds(Math.max(1, cacheGoverned.ttlSeconds())));
            } catch (Exception ignored) {
                // 缓存写入失败不影响本次已经成功的模型调用。
            }
        }
        return result;
    }

    private String digest(Object[] args) {
        try {
            byte[] bytes = objectMapper.writeValueAsString(args).getBytes(StandardCharsets.UTF_8);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        } catch (Exception exception) {
            return Integer.toHexString(java.util.Arrays.deepHashCode(args));
        }
    }
}
