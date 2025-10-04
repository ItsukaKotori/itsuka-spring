package com.itsuka.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求限流过滤器
 *
 * @author itsuka
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(ReactiveStringRedisTemplate.class)
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 限流配置 - 每分钟最多100个请求
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String clientId = getClientId(request);
        String redisKey = "rate_limit:" + clientId;

        return redisTemplate.opsForValue()
                .get(redisKey)
                .switchIfEmpty(Mono.just("0"))
                .cast(String.class)
                .flatMap(currentCount -> {
                    int count = Integer.parseInt(currentCount);
                    
                    if (count >= MAX_REQUESTS_PER_MINUTE) {
                        log.warn("Rate limit exceeded for client: {}, current count: {}", clientId, count);
                        return rateLimitExceededResponse(exchange);
                    }

                    // 增加计数
                    return redisTemplate.opsForValue()
                            .increment(redisKey)
                            .flatMap(newCount -> {
                                if (newCount == 1) {
                                    // 第一次访问，设置过期时间
                                    return redisTemplate.expire(redisKey, WINDOW_DURATION)
                                            .then(chain.filter(exchange));
                                } else {
                                    return chain.filter(exchange);
                                }
                            });
                })
                .onErrorResume(throwable -> {
                    log.error("Rate limit check failed for client: {}", clientId, throwable);
                    // Redis出错时放行请求
                    return chain.filter(exchange);
                });
    }

    /**
     * 获取客户端标识
     */
    private String getClientId(ServerHttpRequest request) {
        // 优先使用用户ID（从认证过滤器传递）
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }

        // 使用IP地址作为标识
        String clientIp = getClientIp(request);
        return "ip:" + clientIp;
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddress() != null 
                ? request.getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";
    }

    /**
     * 返回限流响应
     */
    private Mono<Void> rateLimitExceededResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + WINDOW_DURATION.toMillis()));

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.put("message", "Too many requests, please try again later");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", exchange.getRequest().getURI().getPath());

        try {
            String responseBody = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error creating rate limit response", e);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        // 在认证过滤器之后执行
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}