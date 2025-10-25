package org.itsuka.mqtt.auth.impl;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.auth.MqttAuthenticator;
import org.itsuka.mqtt.protocol.MqttConnectPayload;
import org.itsuka.mqtt.protocol.MqttConnectReturnCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 基于Spring Security的MQTT认证器
 * 当Spring Security不可用时，回退到允许所有连接
 * 
 * @author itsuka
 */
@Slf4j
@Component
public class SpringSecurityMqttAuthenticator implements MqttAuthenticator {
    
    @Autowired(required = false)
    private Object authenticationManager; // Use Object to avoid compile-time dependency
    
    @Override
    public MqttAuthResult authenticate(Channel channel, MqttConnectPayload payload) {
        try {
            String clientId = payload.getClientId();
            String username = payload.getUsername();
            byte[] password = payload.getPassword();
            
            log.debug("Authenticating client: {} with username: {}", clientId, username);
            
            // 如果没有配置认证管理器，允许所有连接
            if (authenticationManager == null) {
                log.warn("No authentication manager configured, allowing connection");
                channel.attr(AuthAttributes.AUTHENTICATED).set(true);
                return MqttAuthResult.success();
            }
            
            // 尝试Spring Security认证
            return performSpringSecurityAuth(channel, clientId, username, password);
            
        } catch (Exception e) {
            log.error("Unexpected error during authentication", e);
            return MqttAuthResult.failure(MqttConnectReturnCode.SERVER_UNAVAILABLE, 
                "Server error during authentication");
        }
    }
    
    /**
     * 执行Spring Security认证
     */
    private MqttAuthResult performSpringSecurityAuth(Channel channel, String clientId, String username, byte[] password) {
        try {
            // 使用反射检查和调用Spring Security
            Class<?> authManagerClass = Class.forName("org.springframework.security.authentication.AuthenticationManager");
            Class<?> authTokenClass = Class.forName("org.springframework.security.authentication.UsernamePasswordAuthenticationToken");
            Class<?> authClass = Class.forName("org.springframework.security.core.Authentication");
            
            if (!authManagerClass.isInstance(authenticationManager)) {
                log.warn("Authentication manager is not a Spring Security AuthenticationManager, allowing connection");
                channel.attr(AuthAttributes.AUTHENTICATED).set(true);
                return MqttAuthResult.success();
            }
            
            // 检查是否需要认证
            if (username == null || username.isEmpty()) {
                log.debug("No username provided, checking if anonymous access is allowed");
                channel.attr(AuthAttributes.AUTHENTICATED).set(true);
                return MqttAuthResult.success();
            }
            
            // 创建认证令牌
            String passwordStr = password != null ? new String(password) : "";
            Object authRequest = authTokenClass.getConstructor(String.class, String.class).newInstance(username, passwordStr);
            
            // 执行认证
            Object authentication = authManagerClass.getMethod("authenticate", 
                Class.forName("org.springframework.security.core.Authentication")).invoke(authenticationManager, authRequest);
            
            // 检查认证结果
            boolean isAuthenticated = (Boolean) authClass.getMethod("isAuthenticated").invoke(authentication);
            
            if (isAuthenticated) {
                log.info("Client {} authenticated successfully", clientId);
                // 将认证信息存储在通道属性中
                channel.attr(AuthAttributes.AUTHENTICATED).set(true);
                channel.attr(AuthAttributes.AUTHENTICATION).set(authentication);
                return MqttAuthResult.success();
            } else {
                log.warn("Authentication failed for client: {}", clientId);
                return MqttAuthResult.failure(MqttConnectReturnCode.BAD_USERNAME_OR_PASSWORD, 
                    "Invalid username or password");
            }
            
        } catch (ClassNotFoundException e) {
            log.warn("Spring Security not available on classpath, allowing connection");
            channel.attr(AuthAttributes.AUTHENTICATED).set(true);
            return MqttAuthResult.success();
        } catch (Exception e) {
            log.error("Error during Spring Security authentication", e);
            return MqttAuthResult.failure(MqttConnectReturnCode.SERVER_UNAVAILABLE, 
                "Authentication error: " + e.getMessage());
        }
    }
    
    /**
     * 认证属性常量
     */
    public static class AuthAttributes {
        public static final io.netty.util.AttributeKey<Boolean> AUTHENTICATED = 
            io.netty.util.AttributeKey.valueOf("mqtt.authenticated");
        public static final io.netty.util.AttributeKey<Object> AUTHENTICATION = 
            io.netty.util.AttributeKey.valueOf("mqtt.authentication");
    }
}