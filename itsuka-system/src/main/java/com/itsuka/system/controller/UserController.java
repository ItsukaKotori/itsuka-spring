package com.itsuka.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsuka.system.domain.SysUser;
import com.itsuka.system.dto.user.UserCreateDto;
import com.itsuka.system.dto.user.UserQueryCriteria;
import com.itsuka.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itsuka.core.domian.Response;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * UserController
 *
 * @author Itsuka
 * @since  2023/3/18
 */
@Tag(name = "用户相关接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final SysUserService userService;

    @GetMapping("/page")
    @Operation(summary = "用户分页", description = "用户分页")
    public Response<Page<SysUser>> page(Page<SysUser> page, UserQueryCriteria criteria) {
        return Response.success(userService.pageUser(page, criteria));
    }

    @PutMapping
    @Operation(summary = "用户新增", description = "用户新增")
    public Response<Boolean> create(@RequestBody @Validated UserCreateDto createDto) {
        return Response.success(userService.create(createDto));
    }
}
