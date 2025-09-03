package com.itsuka.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itsuka.system.domain.LoginUser;
import com.itsuka.system.domain.SysUser;
import com.itsuka.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.itsuka.core.exception.ServiceException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 用户详情服务实现类
 *
 * @author Itsuka
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final SysUserService sysUserService;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 根据用户名查询用户
        SysUser user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUsername, username));
        
        // 如果用户不存在，抛出异常
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        
        // 如果用户被禁用，抛出异常
        if (!user.getEnable()) {
            throw ServiceException.create(403, "用户已被禁用");
        }
        
        // 如果用户被锁定，抛出异常
        if (user.getLock()) {
            throw ServiceException.create(403, "用户已被锁定");
        }
        
        // 简化实现，实际项目中需要从数据库查询用户角色和权限
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        
        // 返回LoginUser对象
        return new LoginUser(user, authorities);
    }
}