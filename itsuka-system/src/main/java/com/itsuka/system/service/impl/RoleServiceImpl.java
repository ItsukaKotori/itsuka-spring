package com.itsuka.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itsuka.system.convert.RoleConvert;
import com.itsuka.system.domain.ResourceRole;
import com.itsuka.system.domain.Role;
import com.itsuka.system.dto.role.RoleCreateDto;
import com.itsuka.system.dto.role.RoleQueryCriteria;
import com.itsuka.system.dto.role.RoleUpdateDto;
import com.itsuka.system.mapper.RoleMapper;
import com.itsuka.system.service.ResourceRoleService;
import com.itsuka.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.itsuka.mybatis.plus.dto.PageParam;
import org.itsuka.mybatis.plus.dto.PageResult;
import org.itsuka.mybatis.plus.util.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.stream.Collectors;

/**
 * SysRoleServiceImpl
 *
 * @author itsuka
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role>
    implements RoleService {

    private final ResourceRoleService resourceRoleService;

    @Override
    public PageResult<Role> pageRole(PageParam pageParam, RoleQueryCriteria criteria) {
        Page<Role> page = page(PageUtil.toPage(pageParam), getWrapper(criteria));
        return PageUtil.toResult(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean create(RoleCreateDto createDto) {
        Role role = RoleConvert.INSTANCE.convert(createDto);
        this.save(role);
        if (!ObjectUtils.isEmpty(createDto.getMenuIds())) {
            resourceRoleService.saveBatch(createDto.getMenuIds().stream()
                    .map(resourceId -> new ResourceRole().setResourceId(resourceId).setRoleId(role.getId()))
                    .collect(Collectors.toList()));
        }
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(RoleUpdateDto updateDto) {
        Role role = this.getById(updateDto.getId());
        if (ObjectUtils.isEmpty(role)) {
            throw new RuntimeException("角色不存在!");
        }
        Role updateRole = RoleConvert.INSTANCE.convert(updateDto);
        this.update(updateRole, new LambdaQueryWrapper<Role>()
                .eq(Role::getId, updateDto.getId()));
        
        // 更新角色资源关系
        resourceRoleService.remove(new LambdaQueryWrapper<ResourceRole>()
                .eq(ResourceRole::getRoleId, updateDto.getId()));
        
        if (!ObjectUtils.isEmpty(updateDto.getMenuIds())) {
            resourceRoleService.saveBatch(updateDto.getMenuIds().stream()
                    .map(resourceId -> new ResourceRole().setResourceId(resourceId).setRoleId(updateDto.getId()))
                    .collect(Collectors.toList()));
        }
        
        return Boolean.TRUE;
    }

    private Wrapper<Role> getWrapper(RoleQueryCriteria criteria) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(!ObjectUtils.isEmpty(criteria.getCode()),
                Role::getCode, criteria.getCode());
        wrapper.like(!ObjectUtils.isEmpty(criteria.getName()),
                Role::getName, criteria.getName());
        return wrapper;
    }
}




