package com.itsuka.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itsuka.system.domain.Resource;
import com.itsuka.system.dto.resource.ResourceCreateDto;
import com.itsuka.system.dto.resource.ResourceQueryCriteria;
import com.itsuka.system.dto.resource.ResourceUpdateDto;

import java.util.List;

/**
 * SysResourceService
 *
 * @author itsuka
 */
public interface ResourceService extends IService<Resource> {
    /**
     * 分页查询
     *
     * @param page 分页条件
     * @param criteria 查询条件
     * @return 分页
     */
    Page<Resource> pageResource(Page<Resource> page, ResourceQueryCriteria criteria);

    /**
     * 创建菜单
     *
     * @param createDto 菜单创建实体
     * @return Boolean
     */
    Boolean create(ResourceCreateDto createDto);

    /**
     * 更新菜单
     * 
     * @param updateDto 菜单更新实体
     * @return Boolean
     */
    Boolean update(ResourceUpdateDto updateDto);

    /**
     * 根据角色ID获取资源列表
     *
     * @param roleId 角色ID
     * @return 资源列表
     */
    List<Resource> getResourceByRoleId(Long roleId);
}
