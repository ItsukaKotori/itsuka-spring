package com.itsuka.gateway.service;

import com.itsuka.gateway.client.SystemServiceClient;
import com.itsuka.security.contract.UserDetailsProvider;
import com.itsuka.security.model.SecurityUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 网关用户详情服务 - 通过简单实现提供用户认证
 *
 * @author Itsuka
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayUserDetailsService implements UserDetailsProvider {
    
    private final SystemServiceClient systemServiceClient;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            log.debug("Loading user by username: {}", username);
            
            // 调用系统模块的用户服务进行用户查询
            SystemServiceClient.UserInfo userInfo = systemServiceClient
                    .getUserByUsername(username)
                    .block(Duration.ofSeconds(5));
            
            if (userInfo == null) {
                throw new UsernameNotFoundException("用户不存在: " + username);
            }
            
            return SecurityUserDetails.builder()
                    .userId(userInfo.getId())
                    .username(userInfo.getUsername())
                    .password(userInfo.getPassword())
                    .enabled(Boolean.TRUE.equals(userInfo.getEnabled()))
                    .accountNonExpired(true)
                    .accountNonLocked(!Boolean.TRUE.equals(userInfo.getLocked()))
                    .credentialsNonExpired(true)
                    .authorities(userInfo.getAuthorities())
                    .build();
        } catch (Exception e) {
            log.error("Failed to load user: {}", username, e);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
    }
    
    @Override
    public boolean validatePassword(String username, String password) {
        try {
            log.debug("Validating password for user: {}", username);
            
            // 调用系统模块进行密码验证
            Boolean result = systemServiceClient
                    .validatePassword(username, password)
                    .block(Duration.ofSeconds(5));
            
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to validate password for user: {}", username, e);
            return false;
        }
    }
    
    @Override
    public UserDetails loadUserById(String userId) {
        try {
            log.debug("Loading user by ID: {}", userId);
            
            // 调用系统模块根据用户ID查询用户
            SystemServiceClient.UserInfo userInfo = systemServiceClient
                    .getUserById(userId)
                    .block(Duration.ofSeconds(5));
            
            if (userInfo == null) {
                throw new UsernameNotFoundException("用户不存在: " + userId);
            }
            
            return SecurityUserDetails.builder()
                    .userId(userInfo.getId())
                    .username(userInfo.getUsername())
                    .password(userInfo.getPassword())
                    .enabled(Boolean.TRUE.equals(userInfo.getEnabled()))
                    .accountNonExpired(true)
                    .accountNonLocked(!Boolean.TRUE.equals(userInfo.getLocked()))
                    .credentialsNonExpired(true)
                    .authorities(userInfo.getAuthorities())
                    .build();
        } catch (Exception e) {
            log.error("Failed to load user by ID: {}", userId, e);
            throw new UsernameNotFoundException("用户不存在: " + userId);
        }
    }
}