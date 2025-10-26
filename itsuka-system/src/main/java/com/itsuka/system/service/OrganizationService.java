package com.itsuka.system.service;

import com.itsuka.system.domain.Organization;
import com.itsuka.system.dto.organization.OrganizationCreateDto;
import com.itsuka.system.dto.organization.OrganizationQueryCriteria;
import com.itsuka.system.dto.organization.OrganizationTreeDto;
import com.itsuka.system.dto.organization.OrganizationUpdateDto;
import org.itsuka.mybatis.plus.dto.PageParam;
import org.itsuka.mybatis.plus.dto.PageResult;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 组织机构服务接口
 *
 * @author itsuka
 */
public interface OrganizationService extends IService<Organization> {

    /**
     * 分页查询组织机构
     *
     * @param pageParam 分页参数
     * @param criteria 查询条件
     * @return 分页结果
     */
    PageResult<Organization> pageOrganization(PageParam pageParam, OrganizationQueryCriteria criteria);

    /**
     * 获取组织机构树形结构
     *
     * @return 组织机构树
     */
    List<OrganizationTreeDto> getOrganizationTree();

    /**
     * 根据父级ID获取子节点
     *
     * @param parentId 父级ID
     * @return 子节点列表
     */
    List<Organization> getChildrenByParentId(Long parentId);

    /**
     * 创建组织机构
     *
     * @param createDto 创建DTO
     * @return 是否成功
     */
    Boolean create(OrganizationCreateDto createDto);

    /**
     * 更新组织机构
     *
     * @param updateDto 更新DTO
     * @return 是否成功
     */
    Boolean update(OrganizationUpdateDto updateDto);

    /**
     * 删除组织机构
     *
     * @param id 组织机构ID
     * @return 是否成功
     */
    Boolean delete(Long id);

    /**
     * 检查编码是否存在
     *
     * @param code 编码
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    Boolean checkCodeExists(String code, Long excludeId);

    /**
     * 根据类型获取组织机构列表
     *
     * @param type 组织类型
     * @return 组织机构列表
     */
    List<Organization> getByType(String type);

    /**
     * 移动组织机构（更改父级）
     *
     * @param id 组织机构ID
     * @param newParentId 新父级ID
     * @return 是否成功
     */
    Boolean moveOrganization(Long id, Long newParentId);

    /**
     * 获取部门下的用户列表
     *
     * @param organizationId 部门ID
     * @return 用户列表
     */
    List<com.itsuka.system.domain.User> getUsersByOrganization(Long organizationId);
}