package com.itsuka.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 熔断降级处理器
 *
 * @author itsuka
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * 系统服务降级处理
     */
    @RequestMapping("/system")
    public ResponseEntity<Map<String, Object>> systemFallback() {
        log.warn("System service circuit breaker triggered");
        return createFallbackResponse("系统服务暂时不可用，请稍后重试");
    }

    /**
     * 通用降级处理
     */
    @RequestMapping("/default")
    public ResponseEntity<Map<String, Object>> defaultFallback() {
        log.warn("Default circuit breaker triggered");
        return createFallbackResponse("服务暂时不可用，请稍后重试");
    }

    /**
     * 创建降级响应
     */
    private ResponseEntity<Map<String, Object>> createFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 503);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        response.put("fallback", true);
        
        return ResponseEntity.status(503).body(response);
    }
}