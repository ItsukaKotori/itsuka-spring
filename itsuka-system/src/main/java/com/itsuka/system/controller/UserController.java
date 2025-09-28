package com.itsuka.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itsuka.system.domain.User;
import com.itsuka.system.dto.user.UserCreateDto;
import com.itsuka.system.dto.user.UserQueryCriteria;
import com.itsuka.system.dto.user.UserUpdateDto;
import com.itsuka.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.itsuka.core.domain.Response;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
    private final UserService userService;

    @GetMapping("/page")
    @Operation(summary = "用户分页", description = "用户分页")
    public Response<Page<User>> page(Page<User> page, UserQueryCriteria criteria) {
        return Response.success(userService.pageUser(page, criteria));
    }

    @PostMapping
    @Operation(summary = "用户新增", description = "用户新增")
    public Response<Boolean> create(@RequestBody @Validated UserCreateDto createDto) {
        return Response.success(userService.create(createDto));
    }

    @PutMapping
    @Operation(summary = "用户更新", description = "用户更新")
    public Response<Boolean> update(@RequestBody @Validated UserUpdateDto updateDto) {        
        return Response.success(userService.update(updateDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "用户删除", description = "用户删除")
    public Response<Boolean> delete(@PathVariable Long id) {
        return Response.success(userService.removeById(id));
    }
}
