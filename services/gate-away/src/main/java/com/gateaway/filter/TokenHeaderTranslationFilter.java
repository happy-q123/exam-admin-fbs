package com.gateaway.filter;

import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * @author zzq
 * @date 2026-06-11
 */
@Component
@Order(-200)
public class TokenHeaderTranslationFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String authHeader = request.getHeaders().getFirst("Authorization");
        String customToken = request.getHeaders().getFirst("token");

        if ((authHeader == null || authHeader.trim().isEmpty()) && (customToken != null && !customToken.trim().isEmpty())) {
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("Authorization", "Bearer " + customToken)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        return chain.filter(exchange);
    }
}
