package com.itsuka.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 网关信息控制器
 *
 * @author itsuka
 */
@RestController
@RequestMapping("/gateway")
public class SimpleGatewayController {

    /**
     * 网关信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Itsuka Gateway");
        info.put("description", "Spring Cloud Gateway for Itsuka System");
        info.put("version", "1.0.0");
        info.put("author", "itsuka");
        info.put("build-time", LocalDateTime.now());
        
        Map<String, Object> features = new HashMap<>();
        features.put("authentication", "JWT Token Based");
        features.put("rate-limiting", "Redis Based (Optional)");
        features.put("circuit-breaker", "Spring Cloud Circuit Breaker");
        features.put("load-balancing", "Spring Cloud LoadBalancer");
        features.put("cors", "Enabled");
        features.put("logging", "Request/Response Logging");
        
        Map<String, Object> services = new HashMap<>();
        services.put("system-service", "http://localhost:8080 - 系统管理服务");
        services.put("auth-service", "http://localhost:8080 - 认证服务");
        
        info.put("features", features);
        info.put("services", services);
        
        return ResponseEntity.ok(info);
    }
}