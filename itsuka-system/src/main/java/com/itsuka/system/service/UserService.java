package com.itsuka.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itsuka.system.domain.User;
import com.itsuka.system.dto.user.UserCreateDto;
import com.itsuka.system.dto.user.UserQueryCriteria;
import com.itsuka.system.dto.user.UserUpdateDto;

/**
 * SysUserService
 *
 * @author itsuka
 */
public interface UserService extends IService<User> {
    /**
     * 分页查询
     *
     * @param page 分页条件
     * @param criteria 查询条件
     * @return 分页
     */
    Page<User> pageUser(Page<User> page, UserQueryCriteria criteria);

    /**
     * 创建用户
     *
     * @param createDto 用户创建实体
     * @return Boolean
     */
    Boolean create(UserCreateDto createDto);

    /**
     * 更新用户
     * 
     * @param updateDto 用户更新实体
     * @return Boolean
     */
    Boolean update(UserUpdateDto updateDto);
}
