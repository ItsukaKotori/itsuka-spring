package com.itsuka.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itsuka.system.convert.OrganizationConvert;
import com.itsuka.system.domain.Organization;
import com.itsuka.system.domain.User;
import com.itsuka.system.dto.organization.OrganizationCreateDto;
import com.itsuka.system.dto.organization.OrganizationQueryCriteria;
import com.itsuka.system.dto.organization.OrganizationTreeDto;
import com.itsuka.system.dto.organization.OrganizationUpdateDto;
import com.itsuka.system.mapper.OrganizationMapper;
import com.itsuka.system.mapper.UserMapper;
import com.itsuka.system.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.itsuka.mybatis.plus.dto.PageParam;
import org.itsuka.mybatis.plus.dto.PageResult;
import org.itsuka.mybatis.plus.util.PageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 组织机构服务实现类
 *
 * @author itsuka
 */
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl extends ServiceImpl<OrganizationMapper, Organization> implements OrganizationService {

    private final OrganizationMapper organizationMapper;
    private final OrganizationConvert organizationConvert;
    private final UserMapper userMapper;

    @Override
    public PageResult<Organization> pageOrganization(PageParam pageParam, OrganizationQueryCriteria criteria) {
        LambdaQueryWrapper<Organization> queryWrapper = Wrappers.lambdaQuery(Organization.class)
                .like(StringUtils.hasText(criteria.getName()), Organization::getName, criteria.getName())
                .like(StringUtils.hasText(criteria.getCode()), Organization::getCode, criteria.getCode())
                .eq(StringUtils.hasText(criteria.getType()), Organization::getType, criteria.getType())
                .eq(criteria.getParentId() != null, Organization::getParentId, criteria.getParentId())
                .eq(criteria.getStatus() != null, Organization::getStatus, criteria.getStatus())
                .orderByAsc(Organization::getSortOrder)
                .orderByAsc(Organization::getId);
        
        Page<Organization> page = page(PageUtil.toPage(pageParam), queryWrapper);
        return PageUtil.toResult(page);
    }

    @Override
    public List<OrganizationTreeDto> getOrganizationTree() {
        // 使用BaseMapper的selectList方法查询所有组织机构
        LambdaQueryWrapper<Organization> queryWrapper = Wrappers.lambdaQuery(Organization.class)
                .orderByAsc(Organization::getSortOrder)
                .orderByAsc(Organization::getId);
        List<Organization> allOrganizations = list(queryWrapper);
        
        if (CollectionUtils.isEmpty(allOrganizations)) {
            return Collections.emptyList();
        }
        
        // 转换为树形DTO
        List<OrganizationTreeDto> treeDtos = organizationConvert.convertToTreeDtoList(allOrganizations);
        
        // 构建树形结构
        return buildTree(treeDtos, 0L);
    }

    @Override
    public List<Organization> getChildrenByParentId(Long parentId) {
        // 使用BaseMapper的selectList方法查询子节点
        LambdaQueryWrapper<Organization> queryWrapper = Wrappers.lambdaQuery(Organization.class)
                .eq(Organization::getParentId, parentId)
                .orderByAsc(Organization::getSortOrder)
                .orderByAsc(Organization::getId);
        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean create(OrganizationCreateDto createDto) {
        // 检查编码是否存在
        if (checkCodeExists(createDto.getCode(), null)) {
            throw new RuntimeException("组织编码已存在");
        }
        
        Organization organization = organizationConvert.convert(createDto);
        return save(organization);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(OrganizationUpdateDto updateDto) {
        // 检查编码是否存在
        if (checkCodeExists(updateDto.getCode(), updateDto.getId())) {
            throw new RuntimeException("组织编码已存在");
        }
        
        Organization organization = organizationConvert.convert(updateDto);
        return updateById(organization);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id) {
        // 使用BaseMapper的selectCount方法检查是否有子节点
        LambdaQueryWrapper<Organization> queryWrapper = Wrappers.lambdaQuery(Organization.class)
                .eq(Organization::getParentId, id);
        long childCount = count(queryWrapper);
        
        if (childCount > 0) {
            throw new RuntimeException("存在子节点，无法删除");
        }
        
        return removeById(id);
    }

    @Override
    public Boolean checkCodeExists(String code, Long excludeId) {
        int count = organizationMapper.countByCode(code, excludeId);
        return count > 0;
    }

    @Override
    public List<Organization> getByType(String type) {
        // 使用BaseMapper的selectList方法根据类型查询
        LambdaQueryWrapper<Organization> queryWrapper = Wrappers.lambdaQuery(Organization.class)
                .eq(Organization::getType, type)
                .orderByAsc(Organization::getSortOrder)
                .orderByAsc(Organization::getId);
        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean moveOrganization(Long id, Long newParentId) {
        // 检查是否会形成循环引用
        if (isCircularReference(id, newParentId)) {
            throw new RuntimeException("不能移动到自己的子节点下");
        }
        
        Organization organization = getById(id);
        if (organization == null) {
            throw new RuntimeException("组织机构不存在");
        }
        
        organization.setParentId(newParentId);
        return updateById(organization);
    }

    @Override
    public List<User> getUsersByOrganization(Long organizationId) {
        // 直接使用UserMapper查询，避免循环依赖
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery(User.class)
                .eq(User::getOrganizationId, organizationId);
        return userMapper.selectList(queryWrapper);
    }

    /**
     * 构建树形结构
     *
     * @param allNodes 所有节点
     * @param parentId 父级ID
     * @return 树形结构
     */
    private List<OrganizationTreeDto> buildTree(List<OrganizationTreeDto> allNodes, Long parentId) {
        return allNodes.stream()
                .filter(node -> Objects.equals(node.getParentId(), parentId))
                .peek(node -> {
                    List<OrganizationTreeDto> children = buildTree(allNodes, node.getId());
                    node.setChildren(CollectionUtils.isEmpty(children) ? null : children);
                })
                .sorted(Comparator.comparing(OrganizationTreeDto::getSortOrder)
                        .thenComparing(OrganizationTreeDto::getId))
                .collect(Collectors.toList());
    }

    /**
     * 检查是否会形成循环引用
     *
     * @param id 当前节点ID
     * @param parentId 父级ID
     * @return 是否会形成循环引用
     */
    private boolean isCircularReference(Long id, Long parentId) {
        if (parentId == null || parentId.equals(0L)) {
            return false;
        }
        
        if (parentId.equals(id)) {
            return true;
        }
        
        // 使用BaseMapper的selectById方法
        Organization parent = getById(parentId);
        if (parent == null) {
            return false;
        }
        
        return isCircularReference(id, parent.getParentId());
    }
}