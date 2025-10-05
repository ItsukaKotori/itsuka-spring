package com.itsuka.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsuka.gateway.service.AccessControlService;
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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 访问控制过滤器 - 白名单和黑名单
 *
 * @author itsuka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessControlFilter implements GlobalFilter, Ordered {

    private final AccessControlService accessControlService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String clientIp = getClientIp(request);
        String userAgent = request.getHeaders().getFirst("User-Agent");

        log.debug("Access control filter processing - Path: {}, IP: {}, UserAgent: {}", 
                 path, clientIp, userAgent);

        // 检查访问控制
        if (!accessControlService.isAccessAllowed(path, clientIp, userAgent)) {
            log.warn("Access denied - Path: {}, IP: {}, UserAgent: {}", path, clientIp, userAgent);
            return forbiddenResponse(exchange, "Access denied");
        }

        log.debug("Access control passed for path: {}", path);
        return chain.filter(exchange);
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIp(ServerHttpRequest request) {
        // 优先从X-Forwarded-For头获取
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For可能包含多个IP，取第一个
            return xForwardedFor.split(",")[0].trim();
        }

        // 从X-Real-IP头获取
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        // 从Proxy-Client-IP头获取
        String proxyClientIp = request.getHeaders().getFirst("Proxy-Client-IP");
        if (proxyClientIp != null && !proxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(proxyClientIp)) {
            return proxyClientIp;
        }

        // 从WL-Proxy-Client-IP头获取
        String wlProxyClientIp = request.getHeaders().getFirst("WL-Proxy-Client-IP");
        if (wlProxyClientIp != null && !wlProxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(wlProxyClientIp)) {
            return wlProxyClientIp;
        }

        // 最后从远程地址获取
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    /**
     * 返回403 Forbidden响应
     */
    private Mono<Void> forbiddenResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", HttpStatus.FORBIDDEN.value());
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", exchange.getRequest().getURI().getPath());

        try {
            String responseBody = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error creating forbidden response", e);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        // 在日志过滤器之后、认证过滤器之前执行
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}