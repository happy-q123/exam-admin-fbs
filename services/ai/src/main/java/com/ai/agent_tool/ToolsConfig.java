package com.ai.agent_tool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

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
}
