package com.itsuka.gateway.filter;

import com.itsuka.gateway.response.ErrorResponse;
import com.itsuka.gateway.utils.ResponseGenerator;
import com.itsuka.security.manager.AccessControlManager;
import com.itsuka.security.toolkit.IpAddressToolkit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 访问控制过滤器 - 白名单和黑名单
 *
 * @author itsuka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessControlFilter implements GlobalFilter, Ordered {

    private final AccessControlManager accessControlManager;
    private final ResponseGenerator responseGenerator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String clientIp = IpAddressToolkit.getClientIp(request);
        String userAgent = request.getHeaders().getFirst("User-Agent");

        log.debug("Access control filter processing - Path: {}, IP: {}, UserAgent: {}", 
                 path, IpAddressToolkit.formatIpForLog(clientIp), userAgent);

        // 检查访问控制
        if (!accessControlManager.isAccessAllowed(path, clientIp, userAgent)) {
            log.warn("Access denied - Path: {}, IP: {}, UserAgent: {}", 
                    path, IpAddressToolkit.formatIpForLog(clientIp), userAgent);
            return responseGenerator.writeTo(exchange, ErrorResponse.forbidden(path, "Access denied"));
        }

        log.debug("Access control passed for path: {}", path);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 在日志过滤器之后、认证过滤器之前执行
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}