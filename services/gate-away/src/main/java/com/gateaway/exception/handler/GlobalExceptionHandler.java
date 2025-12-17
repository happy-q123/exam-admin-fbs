package com.gateaway.exception.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关全局异常处理
 * Order(-1) 优先级必须高于默认的异常处理器
 */
@Configuration
@Order(-1)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        // 1. 如果响应已经提交，直接结束
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // 2. 设置响应头类型为 JSON
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 3. 判断异常类型，提取状态码和错误信息
        if (ex instanceof ResponseStatusException) {
            // 比如 404 Not Found 会进这里
            response.setStatusCode(((ResponseStatusException) ex).getStatusCode());
        } else {
            // 其他未知异常，统一 500
            // response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR); // 可选
        }

        // 4. 构建返回的 JSON 数据
        Map<String, Object> result = new HashMap<>();
        // 获取当前设置的状态码（如果没有就是 500）
        int code = response.getStatusCode() != null ? response.getStatusCode().value() : 500;
        
        result.put("code", code);
        // 这里可以根据异常类型做更细致的判断，比如 "服务不可用"、"路由不存在"
        result.put("msg", "网关异常: " + ex.getMessage()); 
        result.put("data", null);

        // 5. 将 Map 转为 JSON 字节流并写入响应
        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();
            try {
                return bufferFactory.wrap(objectMapper.writeValueAsBytes(result));
            } catch (JsonProcessingException e) {
                return bufferFactory.wrap(new byte[0]);
            }
        }));
    }
}