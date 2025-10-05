package com.itsuka.gateway.filter;

import com.itsuka.gateway.utils.ResponseGenerator;
import com.itsuka.security.toolkit.IpAddressToolkit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.core.domain.RateLimitErrorResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

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
    private final ResponseGenerator responseGenerator;

    // 限流配置 - 每分钟最多100个请求
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
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
                        return responseGenerator.writeTo(exchange, RateLimitErrorResponse.tooManyRequests(
                                path,
                                MAX_REQUESTS_PER_MINUTE,
                                System.currentTimeMillis() + WINDOW_DURATION.toMillis()));
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
        String clientIp = IpAddressToolkit.getClientIp(request);
        return "ip:" + clientIp;
    }

    @Override
    public int getOrder() {
        // 在认证过滤器之后执行
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}