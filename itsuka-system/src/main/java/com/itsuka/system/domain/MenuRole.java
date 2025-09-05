package com.itsuka.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色菜单关联表
 *
 * @author itsuka
 */
@TableName(value ="menu_role")
@Data
public class MenuRole {
    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 菜单ID
     */
    private Long menuId;
}