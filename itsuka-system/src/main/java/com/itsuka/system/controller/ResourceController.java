package com.itsuka.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsuka.system.domain.Resource;
import com.itsuka.system.dto.resource.ResourceCreateDto;
import com.itsuka.system.dto.resource.ResourceQueryCriteria;
import com.itsuka.system.dto.resource.ResourceUpdateDto;
import com.itsuka.system.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itsuka.core.domain.Response;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资源控制器
 *
 * @author itsuka
 */
@Tag(name = "资源相关接口")
@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {
    private final ResourceService resourceService;

    @GetMapping("/page")
    @Operation(summary = "资源分页", description = "资源分页")
    public Response<Page<Resource>> page(Page<Resource> page, ResourceQueryCriteria criteria) {
        return Response.success(resourceService.pageResource(page, criteria));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取资源详情", description = "获取资源详情")
    public Response<Resource> getById(@PathVariable Long id) {
        return Response.success(resourceService.getById(id));
    }

    @GetMapping
    @Operation(summary = "获取资源列表", description = "获取所有资源列表")
    public Response<List<Resource>> list(ResourceQueryCriteria criteria) {
        // 构建查询条件
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Resource> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (criteria != null) {
            wrapper.like(criteria.getName() != null, Resource::getName, criteria.getName());
            wrapper.like(criteria.getCode() != null, Resource::getCode, criteria.getCode());
            if (criteria.getPid() != null) {
                wrapper.eq(Resource::getPid, criteria.getPid());
            }
        }
        return Response.success(resourceService.list(wrapper));
    }

    @GetMapping("/tree")
    @Operation(summary = "获取资源树", description = "获取资源树结构")
    public Response<List<Resource>> tree() {
        // 实际项目中可能需要递归构建树形结构
        // 这里简化处理，返回所有资源
        return Response.success(resourceService.list());
    }

    @PostMapping
    @Operation(summary = "资源新增", description = "资源新增")
    public Response<Boolean> create(@RequestBody @Validated ResourceCreateDto createDto) {
        return Response.success(resourceService.create(createDto));
    }

    @PutMapping
    @Operation(summary = "资源更新", description = "资源更新")
    public Response<Boolean> update(@RequestBody @Validated ResourceUpdateDto updateDto) {
        return Response.success(resourceService.update(updateDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "资源删除", description = "资源删除")
    public Response<Boolean> delete(@PathVariable Long id) {
        return Response.success(resourceService.removeById(id));
    }

    @GetMapping("/role/{roleId}")
    @Operation(summary = "根据角色ID获取资源", description = "根据角色ID获取资源列表")
    public Response<List<Resource>> getByRoleId(@PathVariable Long roleId) {
        return Response.success(resourceService.getResourceByRoleId(roleId));
    }
}