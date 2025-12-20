package com.auth.config;

import com.auth.mapper.SysOauth2JwkMapper;
import com.domain.entity.SysOauth2Jwk;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.UUID;

@Configuration
public class JwkSetConfiguration {

    private final SysOauth2JwkMapper jwkMapper;

    // 构造注入 Mapper
    public JwkSetConfiguration(SysOauth2JwkMapper jwkMapper) {
        this.jwkMapper = jwkMapper;
    }

    /**
     * 核心：自定义 JWKSource，实现密钥持久化
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = getOrGenerateRsaKey();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    /**
     * 获取或生成 RSA 密钥
     */
    private RSAKey getOrGenerateRsaKey() {
        // 1. 定义一个固定的 ID，确保每次重启都查这一条
        String staticKeyId = "oauth2-rsa-key";

        // 2. 尝试从数据库查询
        SysOauth2Jwk dbJwk = jwkMapper.selectById(staticKeyId);

        if (dbJwk != null) {
            try {
                // 3. 如果数据库存在，直接反序列化
                // Nimbus jose-jwt 提供了 parse 方法，非常方便
                return RSAKey.parse(dbJwk.getJwkJson());
            } catch (Exception e) {
                throw new RuntimeException("数据库中的 RSA 密钥解析失败", e);
            }
        }

        // 4. 如果数据库不存在，生成新的密钥对
        RSAKey newRsaKey = generateRsa();

        // 5. 保存到数据库
        SysOauth2Jwk newJwkEntity = new SysOauth2Jwk();
        newJwkEntity.setId(staticKeyId);
        // 重点：toJSONString() 会包含私钥，一定要保护好数据库
        newJwkEntity.setJwkJson(newRsaKey.toJSONString());
        newJwkEntity.setCreateTime(LocalDateTime.now());
        
        jwkMapper.insert(newJwkEntity);

        return newRsaKey;
    }

    /**
     * 生成 RSA 密钥对的工具方法 (Spring Auth Server 官方示例代码)
     */
    private static RSAKey generateRsa() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString()) // 这里的 ID 是 Key 内部的 ID，不影响数据库主键
                .build();
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }
}