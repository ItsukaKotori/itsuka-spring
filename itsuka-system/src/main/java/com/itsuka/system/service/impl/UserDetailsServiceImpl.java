package com.itsuka.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itsuka.system.domain.User;
import com.itsuka.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.itsuka.core.domain.LoginUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 用户详情服务实现类 - 基于Spring Security标准
 *
 * @author Itsuka
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final UserService userService;
    
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
        var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        
        // 直接返回 LoginUser 对象
        return new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getPassword(), // 密码用于认证验证
                user.getEnable(),
                user.getLock(),
                authorities
        );
    }

}