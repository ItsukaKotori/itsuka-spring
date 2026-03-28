package com.itsuka.gateway.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.core.domain.BaseResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseGenerator {
    private final JsonMapper jsonMapper;

    /**
     * 将响应写入ServerWebExchange
     *
     * @param exchange     ServerWebExchange
     * @param baseResponse 响应对象
     * @return Mono<Void>
     */
    public <T> Mono<Void> writeTo(ServerWebExchange exchange, BaseResponse<T> baseResponse) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(baseResponse.getHttpStatus());
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        // 添加自定义响应头
        // baseResponse.getResponseHeaders().forEach(response.getHeaders()::add);

        String responseBody = jsonMapper.writeValueAsString(baseResponse);
        DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
