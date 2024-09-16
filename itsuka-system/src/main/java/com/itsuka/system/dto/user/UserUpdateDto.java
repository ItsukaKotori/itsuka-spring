package com.itsuka.system.dto.user;

import lombok.Data;

/**
 * UserCreateDto
 *
 * @author pc
 */
@Data
public class UserUpdateDto {
    /**
     * 主键
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 密码
     */
    private String password;

    /**
     * 是否启用
     */
    private Boolean enable;
}
