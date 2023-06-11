package com.itsuka.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itsuka.system.convert.UserConvert;
import com.itsuka.system.domain.SysUser;
import com.itsuka.system.domain.SysUserRole;
import com.itsuka.system.dto.user.UserCreateDto;
import com.itsuka.system.dto.user.UserQueryCriteria;
import com.itsuka.system.dto.user.UserUpdateDto;
import com.itsuka.system.mapper.SysUserMapper;
import com.itsuka.system.service.SysRoleService;
import com.itsuka.system.service.SysUserRoleService;
import com.itsuka.system.service.SysUserService;
import lombok.RequiredArgsConstructor;

import java.util.stream.Collectors;

import org.apache.ibatis.javassist.tools.framedump;
import org.itsuka.core.exception.ServiceException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * SysUserServiceImpl
 *
 * @author pc
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private final BCryptPasswordEncoder passwordEncoder;

    private final SysUserRoleService userRoleService;

    @Override
    public Page<SysUser> pageUser(Page<SysUser> page, UserQueryCriteria criteria) {
        return page(page, getWrapper(criteria));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean create(UserCreateDto createDto) {
        SysUser user = UserConvert.INSTANCE.convert(createDto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        this.save(user);
        if (!ObjectUtils.isEmpty(createDto.getRoleIds())) {
            userRoleService.saveBatch(createDto.getRoleIds().stream()
                    .map(roleId -> new SysUserRole().setRoleId(roleId).setUserId(user.getId()))
                    .collect(Collectors.toList()));
        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean update(UserUpdateDto updateDto) {
        SysUser user = this.getById(updateDto.getId());
        if (ObjectUtils.isEmpty(user)) {
            throw ServiceException.create("用户不存在!");
        }
        SysUser updateUser = UserConvert.INSTANCE.convert(updateDto);
        return this.update(updateUser, new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, updateDto.getId()));
    }

    private Wrapper<SysUser> getWrapper(UserQueryCriteria criteria) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(!ObjectUtils.isEmpty(criteria.getUsername()),
                SysUser::getUsername, criteria.getUsername());
        wrapper.like(!ObjectUtils.isEmpty(criteria.getNickname()),
                SysUser::getNickname, criteria.getNickname());
        wrapper.like(!ObjectUtils.isEmpty(criteria.getEnable()),
                SysUser::getEnable, criteria.getEnable());
        return wrapper;
    }
}
