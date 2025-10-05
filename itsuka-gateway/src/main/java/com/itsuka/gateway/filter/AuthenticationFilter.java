package com.itsuka.gateway.filter;

import com.itsuka.gateway.service.UserCacheService;
import com.itsuka.gateway.utils.ResponseGenerator;
import com.itsuka.security.autoconfigure.SecurityProperties;
import com.itsuka.security.token.TokenProvider;
import com.itsuka.security.toolkit.SecurityToolkit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.core.domain.ErrorResponse;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT认证过滤器 - 使用统一的security starter
 *
 * @author itsuka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {
    private final TokenProvider tokenProvider;

    private final SecurityProperties securityProperties;
    
    private final UserCacheService userCacheService;

    private final ResponseGenerator responseGenerator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Authentication filter processing path: {}", path);

        // 检查是否为排除路径
        if (SecurityToolkit.isExcludedPath(path, securityProperties.getAuth().getExcludedPaths())) {
            log.debug("Path {} is excluded from authentication", path);
            return chain.filter(exchange);
        }

        // 获取Authorization头
        String authHeader = request.getHeaders().getFirst("Authorization");
        
        // 提取JWT令牌
        String token = SecurityToolkit.extractToken(authHeader);
        if (token == null) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return responseGenerator.writeTo(exchange, ErrorResponse.unauthorized(path, "Missing or invalid Authorization header"));
        }

        // 验证token并获取用户上下文
        if (!tokenProvider.validateToken(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            return responseGenerator.writeTo(exchange, ErrorResponse.unauthorized(path, "Invalid JWT token"));
        }

        // 从token中获取用户信息
        String username = tokenProvider.getUsernameFromToken(token);
        String userId = tokenProvider.getUserIdFromToken(token);

        if (!StringUtils.hasText(username) || !StringUtils.hasText(userId)) {
            log.warn("Cannot extract username or userId from token for path: {}", path);
            return responseGenerator.writeTo(exchange, ErrorResponse.unauthorized(path, "Invalid token payload"));
        }
        
        // 校验Redis中的用户信息
        return userCacheService.isUserValid(userId)
                .flatMap(isValid -> {
                    if (!isValid) {
                        log.warn("User not found or invalid in cache for userId: {} on path: {}", userId, path);
                        return responseGenerator.writeTo(exchange, ErrorResponse.unauthorized(path, "User not found or disabled"));
                    }
                    
                    // 刷新用户缓存过期时间
                    return userCacheService.refreshUserCacheExpiration(userId, securityProperties.getJwt().getExpiration())
                            .then(continueWithValidUser(exchange, chain, request, username, userId, token, path));
                });
    }

    /**
     * 继续处理有效用户的请求
     */
    private Mono<Void> continueWithValidUser(ServerWebExchange exchange, GatewayFilterChain chain, 
                                           ServerHttpRequest request, String username, String userId, 
                                           String token, String path) {
        // 添加用户信息到请求头，传递给下游服务
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-Username", username)
                .header("X-Token", token)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        log.debug("Authentication successful for user: {} on path: {}", username, path);
        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        // 在访问控制过滤器之后执行
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}