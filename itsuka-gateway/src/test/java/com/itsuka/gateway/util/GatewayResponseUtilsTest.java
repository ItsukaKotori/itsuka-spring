package com.itsuka.gateway.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 网关响应工具类测试
 *
 * @author itsuka
 */
class GatewayResponseUtilsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testCreateUnauthorizedResponse() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // 测试响应创建不会抛出异常
        assertDoesNotThrow(() -> {
            GatewayResponseUtils.createUnauthorizedResponse(exchange, "Unauthorized access", objectMapper);
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertEquals("application/json", exchange.getResponse().getHeaders().getContentType().toString());
    }

    @Test
    void testCreateForbiddenResponse() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/admin").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        assertDoesNotThrow(() -> {
            GatewayResponseUtils.createForbiddenResponse(exchange, "Access denied", objectMapper);
        });

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void testCreateRateLimitResponse() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/data").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        assertDoesNotThrow(() -> {
            GatewayResponseUtils.createRateLimitResponse(
                    exchange, "Too many requests", objectMapper, 100, System.currentTimeMillis() + 60000);
        });

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
        assertEquals("100", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
        assertEquals("0", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"));
        assertNotNull(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Reset"));
    }

    @Test
    void testCreateErrorResponseWithExtraHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/error").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("X-Custom-Header", "custom-value");

        assertDoesNotThrow(() -> {
            GatewayResponseUtils.createErrorResponse(
                    exchange, HttpStatus.BAD_REQUEST, "Bad request", objectMapper, extraHeaders);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        assertEquals("custom-value", exchange.getResponse().getHeaders().getFirst("X-Custom-Header"));
    }
}