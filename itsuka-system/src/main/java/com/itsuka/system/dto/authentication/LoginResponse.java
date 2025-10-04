package com.itsuka.system.dto.authentication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.itsuka.core.domain.LoginUser;

/**
 * 认证响应模型
 *
 * @author Itsuka
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 令牌类型
     */
    @Builder.Default
    private String tokenType = "Bearer";
    
    /**
     * 过期时间（毫秒）
     */
    private Long expiresIn;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 登录用户信息
     */
    private LoginUser userInfo;
}