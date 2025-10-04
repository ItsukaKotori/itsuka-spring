package com.itsuka.system.dto.authentication;

import lombok.Data;

/**
 * 登录请求模型
 */
@Data
public class LoginRequest {
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}