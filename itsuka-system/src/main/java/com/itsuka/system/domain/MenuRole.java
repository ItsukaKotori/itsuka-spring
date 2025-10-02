package com.itsuka.system.domain;

import lombok.Data;

/**
 * 菜单角色关联实体
 * @author Itsuka
 */
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