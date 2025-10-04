package com.itsuka.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsuka.security.jwt.JwtManager;
import com.itsuka.security.properties.AuthProperties;
import com.itsuka.security.util.AuthUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT认证过滤器 - 使用统一的security starter
 *
 * @author itsuka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtManager jwtManager;
    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Authentication filter processing path: {}", path);

        // 检查是否为排除路径
        if (AuthUtils.isExcludedPath(path, authProperties.getExcludedPaths())) {
            log.debug("Path {} is excluded from authentication", path);
            return chain.filter(exchange);
        }

        // 获取Authorization头
        String authHeader = request.getHeaders().getFirst("Authorization");
        
        // 提取JWT令牌
        String token = AuthUtils.extractToken(authHeader);
        if (token == null) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return unauthorizedResponse(exchange, "Missing or invalid Authorization header");
        }

        // 验证token并获取用户上下文
        if (!jwtManager.validateToken(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            return unauthorizedResponse(exchange, "Invalid JWT token");
        }

        // 从token中获取用户信息
        String username = jwtManager.getUsernameFromToken(token);
        String userId = jwtManager.getUserIdFromToken(token);

        if (!StringUtils.hasText(username)) {
            log.warn("Cannot extract username from token for path: {}", path);
            return unauthorizedResponse(exchange, "Invalid token payload");
        }

        // 添加用户信息到请求头，传递给下游服务
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId != null ? userId : "")
                .header("X-Username", username)
                .header("X-Token", token)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        log.debug("Authentication successful for user: {} on path: {}", username, path);
        return chain.filter(mutatedExchange);
    }

    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", HttpStatus.UNAUTHORIZED.value());
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", exchange.getRequest().getURI().getPath());

        try {
            String responseBody = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error creating error response", e);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        // 在日志过滤器之后执行
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}