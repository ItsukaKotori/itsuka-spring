package com.itsuka.system.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * UserCreateDto
 *
 * @author pc
 */
@Data
public class UserCreateDto {
    /**
     * 用户名
     */
    @NotNull(message = "用户名不能为空!")
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 密码
     */
    @NotNull(message = "密码不能为空!")
    private String password;

    /**
     * 是否启用
     */
    private Boolean enable = false;

    /**
     * 角色列表
     */
    private List<Long> roleIds;
}
