package com.itsuka.gateway.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 网关响应工具类
 * 提供统一的错误响应生成方法
 *
 * @author itsuka
 */
@Slf4j
public class GatewayResponseUtils {

    /**
     * 创建统一的错误响应
     *
     * @param exchange    ServerWebExchange
     * @param httpStatus  HTTP状态码
     * @param message     错误消息
     * @param objectMapper JSON序列化器
     * @return Mono<Void>
     */
    public static Mono<Void> createErrorResponse(ServerWebExchange exchange, 
                                               HttpStatus httpStatus, 
                                               String message, 
                                               ObjectMapper objectMapper) {
        return createErrorResponse(exchange, httpStatus, message, objectMapper, null);
    }

    /**
     * 创建统一的错误响应（带额外头部）
     *
     * @param exchange     ServerWebExchange
     * @param httpStatus   HTTP状态码
     * @param message      错误消息
     * @param objectMapper JSON序列化器
     * @param extraHeaders 额外的响应头
     * @return Mono<Void>
     */
    public static Mono<Void> createErrorResponse(ServerWebExchange exchange, 
                                               HttpStatus httpStatus, 
                                               String message, 
                                               ObjectMapper objectMapper,
                                               Map<String, String> extraHeaders) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        // 添加额外头部
        if (extraHeaders != null && !extraHeaders.isEmpty()) {
            extraHeaders.forEach(response.getHeaders()::add);
        }

        // 构建错误响应体
        Map<String, Object> errorResponse = createErrorResponseBody(
                httpStatus, 
                message, 
                exchange.getRequest().getURI().getPath()
        );

        try {
            String responseBody = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error creating error response", e);
            return response.setComplete();
        }
    }

    /**
     * 创建401未授权响应
     *
     * @param exchange     ServerWebExchange
     * @param message      错误消息
     * @param objectMapper JSON序列化器
     * @return Mono<Void>
     */
    public static Mono<Void> createUnauthorizedResponse(ServerWebExchange exchange, 
                                                       String message, 
                                                       ObjectMapper objectMapper) {
        return createErrorResponse(exchange, HttpStatus.UNAUTHORIZED, message, objectMapper);
    }

    /**
     * 创建403禁止访问响应
     *
     * @param exchange     ServerWebExchange
     * @param message      错误消息
     * @param objectMapper JSON序列化器
     * @return Mono<Void>
     */
    public static Mono<Void> createForbiddenResponse(ServerWebExchange exchange, 
                                                   String message, 
                                                   ObjectMapper objectMapper) {
        return createErrorResponse(exchange, HttpStatus.FORBIDDEN, message, objectMapper);
    }

    /**
     * 创建429限流响应
     *
     * @param exchange     ServerWebExchange
     * @param message      错误消息
     * @param objectMapper JSON序列化器
     * @param maxRequests  最大请求数
     * @param resetTime    重置时间戳
     * @return Mono<Void>
     */
    public static Mono<Void> createRateLimitResponse(ServerWebExchange exchange, 
                                                   String message, 
                                                   ObjectMapper objectMapper,
                                                   int maxRequests,
                                                   long resetTime) {
        Map<String, String> rateLimitHeaders = new HashMap<>();
        rateLimitHeaders.put("X-RateLimit-Limit", String.valueOf(maxRequests));
        rateLimitHeaders.put("X-RateLimit-Remaining", "0");
        rateLimitHeaders.put("X-RateLimit-Reset", String.valueOf(resetTime));

        return createErrorResponse(exchange, HttpStatus.TOO_MANY_REQUESTS, message, objectMapper, rateLimitHeaders);
    }

    /**
     * 创建500内部服务器错误响应
     *
     * @param exchange     ServerWebExchange
     * @param message      错误消息
     * @param objectMapper JSON序列化器
     * @return Mono<Void>
     */
    public static Mono<Void> createInternalServerErrorResponse(ServerWebExchange exchange, 
                                                             String message, 
                                                             ObjectMapper objectMapper) {
        return createErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, message, objectMapper);
    }

    /**
     * 创建标准的错误响应体
     *
     * @param httpStatus HTTP状态码
     * @param message    错误消息
     * @param path       请求路径
     * @return 错误响应体Map
     */
    private static Map<String, Object> createErrorResponseBody(HttpStatus httpStatus, String message, String path) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", httpStatus.value());
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", path);
        errorResponse.put("status", httpStatus.getReasonPhrase());
        return errorResponse;
    }

    /**
     * 创建成功响应
     *
     * @param exchange     ServerWebExchange
     * @param data         响应数据
     * @param objectMapper JSON序列化器
     * @return Mono<Void>
     */
    public static Mono<Void> createSuccessResponse(ServerWebExchange exchange, 
                                                 Object data, 
                                                 ObjectMapper objectMapper) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("code", HttpStatus.OK.value());
        successResponse.put("message", "Success");
        successResponse.put("timestamp", System.currentTimeMillis());
        successResponse.put("data", data);

        try {
            String responseBody = objectMapper.writeValueAsString(successResponse);
            DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error creating success response", e);
            return response.setComplete();
        }
    }
}