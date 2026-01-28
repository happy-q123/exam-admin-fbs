package com.ai.agent_tool;

import com.ai.agent_tool.tools.UserErrorQuestionFunction;
import com.domain.record.agent.UserErrorQuestionRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
/**
 * description 工具类
 * author zzq
 * date 2026/1/28 20:33
*/
//todo 考虑@tool注解的使用
@Configuration
public class ToolsConfig {
    @Bean
    @Description("返回当前时间")//模型根据该信息判断该Function的作用
    public Function<Void,String> timeFunction(){
        return unused -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
//        return new Function<Void, String>() {
//            @Override
//            public String apply(Void unused) {
//                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
//            }
//        };
    }

    @Bean
    @Description("获取当前用户的错题列表。列表每个元素为错题信息。错题信息包括题目题干、用户的答案等")
    public Function<UserErrorQuestionRequest,String> userErrorQuestionsFunction(UserErrorQuestionFunction service){
        return service;
    }

    @Bean
    @Description("获取当前用户的Id")
    public Function<Void, Long> userIdFunction() {
        return unused -> {
            // 1. 获取当前的认证信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 2. 判空并检查 Principal 是否为 Jwt 类型
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                // 3. 从 Claims 中提取 userId (根据你 Token 里的 key，这里假设是 "userId")
                return jwt.getClaim("userId");
            }

            // 如果没获取到，可以抛出异常或返回 null
            throw new RuntimeException("当前上下文未找到用户ID");
        };
    }
}
