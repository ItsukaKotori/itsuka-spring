package com.itsuka.gateway.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

/**
 * 系统服务客户端
 * 用于网关调用系统模块的用户服务
 *
 * @author Itsuka
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemServiceClient {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${app.system-service.url:http://localhost:8081}")
    private String systemServiceUrl;
    
    @Value("${app.system-service.timeout:5s}")
    private Duration timeout;
    
    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    public Mono<UserInfo> getUserByUsername(String username) {
        return webClientBuilder.build()
                .get()
                .uri(systemServiceUrl + "/api/users/username/{username}", username)
                .retrieve()
                .bodyToMono(ApiResponse.class)
                .timeout(timeout)
                .map(response -> {
                    if (response.isSuccess()) {
                        return response.getData();
                    } else {
                        throw new RuntimeException("Failed to get user: " + response.getMessage());
                    }
                })
                .doOnError(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().value() == 404) {
                        log.debug("User not found: {}", username);
                    } else {
                        log.error("Error calling system service for user {}: {}", username, ex.getMessage());
                    }
                })
                .onErrorReturn(createDefaultUser(username, "1"))
                .cast(UserInfo.class);
    }
    
    /**
     * 根据用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public Mono<UserInfo> getUserById(String userId) {
        return webClientBuilder.build()
                .get()
                .uri(systemServiceUrl + "/api/users/{userId}", userId)
                .retrieve()
                .bodyToMono(ApiResponse.class)
                .timeout(timeout)
                .map(response -> {
                    if (response.isSuccess()) {
                        return response.getData();
                    } else {
                        throw new RuntimeException("Failed to get user: " + response.getMessage());
                    }
                })
                .doOnError(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().value() == 404) {
                        log.debug("User not found: {}", userId);
                    } else {
                        log.error("Error calling system service for user {}: {}", userId, ex.getMessage());
                    }
                })
                .onErrorReturn(createDefaultUser("user_" + userId, userId))
                .cast(UserInfo.class);
    }
    
    /**
     * 验证用户密码
     *
     * @param username 用户名
     * @param password 密码
     * @return 验证结果
     */
    public Mono<Boolean> validatePassword(String username, String password) {
        return webClientBuilder.build()
                .post()
                .uri(systemServiceUrl + "/api/users/validate-password")
                .bodyValue(new PasswordValidationRequest(username, password))
                .retrieve()
                .bodyToMono(ApiResponse.class)
                .timeout(timeout)
                .map(response -> {
                    if (response.isSuccess()) {
                        return Boolean.TRUE.equals(response.getData());
                    } else {
                        return false;
                    }
                })
                .doOnError(ex -> log.error("Error validating password for user {}: {}", username, ex.getMessage()))
                .onErrorReturn(false);
    }
    
    /**
     * 创建默认用户（当系统服务不可用时的降级方案）
     */
    private UserInfo createDefaultUser(String username, String userId) {
        log.warn("Using fallback user data for: {}", username);
        return UserInfo.builder()
                .id(userId)
                .username(username)
                .password("$2a$10$dummy.hashed.password")
                .enabled(true)
                .locked(false)
                .authorities(Collections.singleton("ROLE_USER"))
                .build();
    }
    
    /**
     * 用户信息数据传输对象
     */
    @Data
    public static class UserInfo {
        private String id;
        private String username;
        private String password;
        private Boolean enabled;
        private Boolean locked;
        private Set<String> authorities;
        
        public static UserInfoBuilder builder() {
            return new UserInfoBuilder();
        }
        
        public static class UserInfoBuilder {
            private final UserInfo userInfo = new UserInfo();
            
            public UserInfoBuilder id(String id) {
                userInfo.id = id;
                return this;
            }
            
            public UserInfoBuilder username(String username) {
                userInfo.username = username;
                return this;
            }
            
            public UserInfoBuilder password(String password) {
                userInfo.password = password;
                return this;
            }
            
            public UserInfoBuilder enabled(Boolean enabled) {
                userInfo.enabled = enabled;
                return this;
            }
            
            public UserInfoBuilder locked(Boolean locked) {
                userInfo.locked = locked;
                return this;
            }
            
            public UserInfoBuilder authorities(Set<String> authorities) {
                userInfo.authorities = authorities;
                return this;
            }
            
            public UserInfo build() {
                return userInfo;
            }
        }
    }
    
    /**
     * 密码验证请求
     */
    @Data
    private static class PasswordValidationRequest {
        private final String username;
        private final String password;
        
        public PasswordValidationRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
    
    /**
     * API响应封装
     */
    @Data
    private static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
    }
}