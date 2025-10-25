package com.itsuka.system.controller;

import org.itsuka.core.domain.Response;
import org.itsuka.mybatis.plus.dto.PageParam;
import org.itsuka.mybatis.plus.dto.PageResult;
import com.itsuka.system.domain.Role;
import com.itsuka.system.dto.role.RoleCreateDto;
import com.itsuka.system.dto.role.RoleQueryCriteria;
import com.itsuka.system.dto.role.RoleUpdateDto;
import com.itsuka.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itsuka.core.domain.Response;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 角色相关接口
 *
 * @author Itsuka
 * @since 2023/6/11
 */
@Tag(name = "角色相关接口")
@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @GetMapping("/page")
    @Operation(summary = "角色分页", description = "角色分页")
    public Response<PageResult<Role>> page(PageParam pageParam, RoleQueryCriteria criteria) {
        return Response.success(roleService.pageRole(pageParam, criteria));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情", description = "获取角色详情")
    public Response<Role> getById(@PathVariable Long id) {
        return Response.success(roleService.getById(id));
    }

    @PostMapping
    @Operation(summary = "角色新增", description = "角色新增")
    public Response<Boolean> create(@RequestBody @Validated RoleCreateDto createDto) {
        return Response.success(roleService.create(createDto));
    }

    @PutMapping
    @Operation(summary = "角色更新", description = "角色更新")
    public Response<Boolean> update(@RequestBody @Validated RoleUpdateDto updateDto) {
        return Response.success(roleService.update(updateDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "角色删除", description = "角色删除")
    public Response<Boolean> delete(@PathVariable Long id) {
        return Response.success(roleService.removeById(id));
    }
}
