package com.itsuka.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itsuka.security.contract.UserDetailsProvider;
import com.itsuka.security.model.SecurityUserDetails;
import com.itsuka.system.domain.User;
import com.itsuka.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

/**
 * 用户详情服务实现类 - 基于Spring Security标准
 *
 * @author Itsuka
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsProvider {
    
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 根据用户名查询用户
        User user = userService.getOne(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, username));
        
        // 如果用户不存在，抛出异常
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        
        // 构建权限列表
        Set<String> authorities = Collections.singleton("ROLE_USER");
        
        // 构建用户详情
        return SecurityUserDetails.builder()
                .userId(user.getId().toString())
                .username(user.getUsername())
                .password(user.getPassword())
                .enabled(user.getEnable() != null ? user.getEnable() : true)
                .accountNonLocked(user.getLock() == null || !user.getLock())
                .accountNonExpired(true)
                .credentialsNonExpired(true)
                .authorities(authorities)
                .build();
    }
    
    @Override
    public boolean validatePassword(String username, String password) {
        // 根据用户名查询用户
        User user = userService.getOne(new LambdaQueryWrapper<User>()
            .eq(User::getUsername, username));
        
        if (user == null) {
            return false;
        }
        
        // 验证密码
        return passwordEncoder.matches(password, user.getPassword());
    }
    
    @Override
    public UserDetails loadUserById(String userId) {
        try {
            Long id = Long.parseLong(userId);
            User user = userService.getById(id);
            
            if (user == null) {
                throw new UsernameNotFoundException("用户不存在: " + userId);
            }
            
            // 构建权限列表
            Set<String> authorities = Collections.singleton("ROLE_USER");
            
            // 构建用户详情
            return SecurityUserDetails.builder()
                    .userId(user.getId().toString())
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .enabled(user.getEnable() != null ? user.getEnable() : true)
                    .accountNonLocked(user.getLock() == null || !user.getLock())
                    .accountNonExpired(true)
                    .credentialsNonExpired(true)
                    .authorities(authorities)
                    .build();
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("无效的用户ID: " + userId);
        }
    }
}