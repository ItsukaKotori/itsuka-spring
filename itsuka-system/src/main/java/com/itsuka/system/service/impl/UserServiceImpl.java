package com.itsuka.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itsuka.system.convert.UserConvert;
import com.itsuka.system.domain.User;
import com.itsuka.system.domain.UserRole;
import com.itsuka.system.dto.user.UserCreateDto;
import com.itsuka.system.dto.user.UserQueryCriteria;
import com.itsuka.system.dto.user.UserUpdateDto;
import com.itsuka.system.mapper.UserMapper;
import com.itsuka.system.service.UserRoleService;
import com.itsuka.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.itsuka.core.exception.ServiceException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.stream.Collectors;

/**
 * SysUserServiceImpl
 *
 * @author pc
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final BCryptPasswordEncoder passwordEncoder;

    private final UserRoleService userRoleService;

    @Override
    public Page<User> pageUser(Page<User> page, UserQueryCriteria criteria) {
        return page(page, getWrapper(criteria));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean create(UserCreateDto createDto) {
        User user = UserConvert.INSTANCE.convert(createDto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        this.save(user);
        if (!ObjectUtils.isEmpty(createDto.getRoleIds())) {
            userRoleService.saveBatch(createDto.getRoleIds().stream()
                    .map(roleId -> new UserRole().setRoleId(roleId).setUserId(user.getId()))
                    .collect(Collectors.toList()));
        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean update(UserUpdateDto updateDto) {
        User user = this.getById(updateDto.getId());
        if (ObjectUtils.isEmpty(user)) {
            throw ServiceException.create("用户不存在!");
        }
        User updateUser = UserConvert.INSTANCE.convert(updateDto);
        return this.update(updateUser, new LambdaQueryWrapper<User>()
                .eq(User::getId, updateDto.getId()));
    }

    private Wrapper<User> getWrapper(UserQueryCriteria criteria) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(!ObjectUtils.isEmpty(criteria.getUsername()),
                User::getUsername, criteria.getUsername());
        wrapper.like(!ObjectUtils.isEmpty(criteria.getNickname()),
                User::getNickname, criteria.getNickname());
        wrapper.like(!ObjectUtils.isEmpty(criteria.getEnable()),
                User::getEnable, criteria.getEnable());
        return wrapper;
    }
}
