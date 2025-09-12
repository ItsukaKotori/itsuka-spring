package com.itsuka.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itsuka.system.convert.ResourceConvert;
import com.itsuka.system.domain.Resource;
import com.itsuka.system.domain.ResourceRole;
import com.itsuka.system.dto.resource.ResourceCreateDto;
import com.itsuka.system.dto.resource.ResourceQueryCriteria;
import com.itsuka.system.dto.resource.ResourceUpdateDto;
import com.itsuka.system.mapper.ResourceMapper;
import com.itsuka.system.service.ResourceRoleService;
import com.itsuka.system.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SysResourceServiceImpl
 *
 * @author itsuka
 */
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource>
    implements ResourceService {

    private final ResourceRoleService resourceRoleService;

    @Override
    public Page<Resource> pageResource(Page<Resource> page, ResourceQueryCriteria criteria) {
        return page(page, getWrapper(criteria));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean create(ResourceCreateDto createDto) {
        Resource resource = ResourceConvert.INSTANCE.convert(createDto);
        return this.save(resource);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(ResourceUpdateDto updateDto) {
        Resource resource = this.getById(updateDto.getId());
        if (ObjectUtils.isEmpty(resource)) {
            throw new RuntimeException("菜单不存在!");
        }
        Resource updateResource = ResourceConvert.INSTANCE.convert(updateDto);
        return this.update(updateResource, new LambdaQueryWrapper<Resource>()
                .eq(Resource::getId, updateDto.getId()));
    }

    @Override
    public List<Resource> getResourceByRoleId(Long roleId) {
        if (ObjectUtils.isEmpty(roleId)) {
            return Collections.emptyList();
        }
        
        // 查询角色关联的资源ID
        List<ResourceRole> resourceRoles = resourceRoleService.list(new LambdaQueryWrapper<ResourceRole>()
                .eq(ResourceRole::getRoleId, roleId));
        
        if (ObjectUtils.isEmpty(resourceRoles)) {
            return Collections.emptyList();
        }
        
        // 根据资源ID查询资源列表
        List<Long> resourceIds = resourceRoles.stream()
                .map(ResourceRole::getResourceId)
                .collect(Collectors.toList());
        
        return this.list(new LambdaQueryWrapper<Resource>()
                .in(Resource::getId, resourceIds));
    }

    private Wrapper<Resource> getWrapper(ResourceQueryCriteria criteria) {
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(!ObjectUtils.isEmpty(criteria.getName()),
                Resource::getName, criteria.getName());
        wrapper.like(!ObjectUtils.isEmpty(criteria.getCode()),
                Resource::getCode, criteria.getCode());
        if (!ObjectUtils.isEmpty(criteria.getPid())) {
            wrapper.eq(Resource::getPid, criteria.getPid());
        }
        return wrapper;
    }
}




