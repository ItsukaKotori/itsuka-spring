package com.itsuka.system.service;

import org.itsuka.mybatis.plus.dto.PageParam;
import org.itsuka.mybatis.plus.dto.PageResult;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itsuka.system.domain.Role;
import com.itsuka.system.dto.role.RoleCreateDto;
import com.itsuka.system.dto.role.RoleQueryCriteria;
import com.itsuka.system.dto.role.RoleUpdateDto;

/**
 * SysRoleService
 *
 * @author itsuka
 */
public interface RoleService extends IService<Role> {
    /**
     * 分页查询
     *
     * @param pageParam 分页参数
     * @param criteria 查询条件
     * @return 分页结果
     */
    PageResult<Role> pageRole(PageParam pageParam, RoleQueryCriteria criteria);

    /**
     * 创建角色
     *
     * @param createDto 角色创建实体
     * @return Boolean
     */
    Boolean create(RoleCreateDto createDto);

    /**
     * 更新角色
     * 
     * @param updateDto 角色更新实体
     * @return Boolean
     */
    Boolean update(RoleUpdateDto updateDto);
}
