package com.itsuka.system.controller;

import com.itsuka.system.domain.Organization;
import com.itsuka.system.domain.User;
import com.itsuka.system.dto.organization.OrganizationCreateDto;
import com.itsuka.system.dto.organization.OrganizationQueryCriteria;
import com.itsuka.system.dto.organization.OrganizationTreeDto;
import com.itsuka.system.dto.organization.OrganizationUpdateDto;
import com.itsuka.system.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itsuka.core.domain.Response;
import org.itsuka.mybatis.plus.dto.PageParam;
import org.itsuka.mybatis.plus.dto.PageResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织机构控制器
 *
 * @author itsuka
 */
@Tag(name = "组织机构相关接口")
@RestController
@RequestMapping("/api/organization")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping("/tree")
    @Operation(summary = "获取组织机构树", description = "获取完整的组织机构树形结构")
    public Response<List<OrganizationTreeDto>> getTree() {
        return Response.success(organizationService.getOrganizationTree());
    }

    @GetMapping("/page")
    @Operation(summary = "组织机构分页查询", description = "分页查询组织机构列表")
    public Response<PageResult<Organization>> page(PageParam pageParam, OrganizationQueryCriteria criteria) {
        return Response.success(organizationService.pageOrganization(pageParam, criteria));
    }

    @GetMapping("/children/{parentId}")
    @Operation(summary = "获取子节点", description = "根据父级ID获取子节点列表")
    public Response<List<Organization>> getChildren(
            @Parameter(description = "父级ID，0表示顶级") @PathVariable Long parentId) {
        return Response.success(organizationService.getChildrenByParentId(parentId));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "根据类型查询", description = "根据组织类型查询组织机构列表")
    public Response<List<Organization>> getByType(
            @Parameter(description = "组织类型：COMPANY-公司，DEPARTMENT-部门，POSITION-岗位") 
            @PathVariable String type) {
        return Response.success(organizationService.getByType(type));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取组织机构详情", description = "根据ID获取组织机构详细信息")
    public Response<Organization> getById(@Parameter(description = "组织机构ID") @PathVariable Long id) {
        return Response.success(organizationService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建组织机构", description = "创建新的组织机构")
    public Response<Boolean> create(@RequestBody @Validated OrganizationCreateDto createDto) {
        return Response.success(organizationService.create(createDto));
    }

    @PutMapping
    @Operation(summary = "更新组织机构", description = "更新组织机构信息")
    public Response<Boolean> update(@RequestBody @Validated OrganizationUpdateDto updateDto) {
        return Response.success(organizationService.update(updateDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除组织机构", description = "删除指定的组织机构")
    public Response<Boolean> delete(@Parameter(description = "组织机构ID") @PathVariable Long id) {
        return Response.success(organizationService.delete(id));
    }

    @PutMapping("/move/{id}/{newParentId}")
    @Operation(summary = "移动组织机构", description = "将组织机构移动到新的父级下")
    public Response<Boolean> move(
            @Parameter(description = "组织机构ID") @PathVariable Long id,
            @Parameter(description = "新父级ID，0表示顶级") @PathVariable Long newParentId) {
        return Response.success(organizationService.moveOrganization(id, newParentId));
    }

    @GetMapping("/{id}/check-code/{code}")
    @Operation(summary = "检查编码是否存在", description = "检查组织编码是否已存在")
    public Response<Boolean> checkCodeExists(
            @Parameter(description = "组织编码") @PathVariable String code,
            @Parameter(description = "排除的ID，用于更新时检查") @PathVariable(required = false) Long id) {
        return Response.success(organizationService.checkCodeExists(code, id));
    }

    @GetMapping("/{id}/users")
    @Operation(summary = "获取部门用户", description = "获取指定部门下的用户列表")
    public Response<List<User>> getUsersByOrganization(
            @Parameter(description = "部门ID") @PathVariable Long id) {
        return Response.success(organizationService.getUsersByOrganization(id));
    }
}