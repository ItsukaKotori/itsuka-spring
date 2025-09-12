package com.itsuka.system.dto.role;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 角色创建DTO
 *
 * @author itsuka
 */
@Data
public class RoleCreateDto {
    /**
     * 角色编码
     */
    @NotNull(message = "角色编码不能为空!")
    private String code;

    /**
     * 角色名称
     */
    @NotNull(message = "角色名称不能为空!")
    private String name;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 资源ID列表
     */
    private List<Long> menuIds;
}