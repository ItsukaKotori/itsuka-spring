package com.itsuka.gateway.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsuka.gateway.response.BaseResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseGenerator {
    private final ObjectMapper objectMapper;

    /**
     * 将响应写入ServerWebExchange
     *
     * @param exchange     ServerWebExchange
     * @param objectMapper JSON序列化器
     * @return Mono<Void>
     */
    public Mono<Void> writeTo(ServerWebExchange exchange, BaseResponse baseResponse) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(baseResponse.getHttpStatus());
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        // 添加自定义响应头
        baseResponse.getResponseHeaders().forEach(response.getHeaders()::add);

        try {
            String responseBody = objectMapper.writeValueAsString(this);
            DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error serializing response: {}", this.getClass().getSimpleName(), e);
            return response.setComplete();
        }
    }
}
