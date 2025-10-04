package com.itsuka.security.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 扩展的用户详情实现，基于Spring Security标准UserDetails接口
 *
 * @author Itsuka
 */
@Data
@Builder(toBuilder = true)
public class SecurityUserDetails implements UserDetails {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = true;
    
    /**
     * 账户是否未过期
     */
    @Builder.Default
    private boolean accountNonExpired = true;
    
    /**
     * 账户是否未锁定
     */
    @Builder.Default
    private boolean accountNonLocked = true;
    
    /**
     * 凭据是否未过期
     */
    @Builder.Default
    private boolean credentialsNonExpired = true;
    
    /**
     * 权限列表
     */
    private Set<String> authorities;
    
    /**
     * 额外的用户信息
     */
    private Map<String, Object> attributes;
    
    /**
     * JWT令牌（可选，用于已认证的用户）
     */
    private String token;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authorities == null || authorities.isEmpty()) {
            return Set.of();
        }
        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 创建未认证的用户详情
     */
    public static SecurityUserDetails unauthenticated() {
        return SecurityUserDetails.builder()
                .enabled(false)
                .build();
    }
    
    /**
     * 检查用户是否已认证（所有状态都为true）
     */
    public boolean isAuthenticated() {
        return enabled && accountNonExpired && accountNonLocked && credentialsNonExpired;
    }
}