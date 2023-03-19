package com.itsuka.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色菜单关联表
 *
 * @author itsuka
 */
@TableName(value ="sys_menu_role")
@Data
public class SysMenuRole {
    /**
     * 角色ID
     */
    @TableId
    private Long roleId;

    /**
     * 菜单ID
     */
    @TableId
    private Long menuId;
}