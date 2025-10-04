package com.itsuka.security.contract;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * 用户详情提供者契约，基于Spring Security标准UserDetailsService
 * 用于在Starter中定义用户数据获取的契约接口
 *
 * @author Itsuka
 */
public interface UserDetailsProvider extends org.springframework.security.core.userdetails.UserDetailsService {
    
    /**
     * 验证用户密码
     *
     * @param username 用户名
     * @param password 密码
     * @return 是否验证成功
     */
    boolean validatePassword(String username, String password);
    
    /**
     * 根据用户ID加载用户详情
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    UserDetails loadUserById(String userId);
}