package com.itsuka.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 全局请求日志过滤器
 *
 * @author itsuka
 */
@Slf4j
@Component
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = UUID.randomUUID().toString();
        
        // 添加请求ID到请求头
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Request-Id", requestId)
                .build();
        
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();
        
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        
        log.info("Gateway Request Start - ID: {}, Method: {}, URI: {}, RemoteAddr: {}, Time: {}", 
                requestId, 
                request.getMethod(), 
                request.getURI(), 
                request.getRemoteAddress(),
                LocalDateTime.now());
        
        return chain.filter(mutatedExchange)
                .doFinally(signalType -> {
                    long endTime = System.currentTimeMillis();
                    log.info("Gateway Request End - ID: {}, Duration: {}ms, Signal: {}", 
                            requestId, 
                            endTime - startTime, 
                            signalType);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}