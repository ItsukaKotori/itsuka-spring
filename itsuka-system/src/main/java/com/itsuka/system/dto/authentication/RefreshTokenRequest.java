package com.itsuka.system.dto.authentication;

import lombok.Data;

/**
 * 刷新令牌请求模型
 */
@Data
public class RefreshTokenRequest {
    private String refreshToken;
}