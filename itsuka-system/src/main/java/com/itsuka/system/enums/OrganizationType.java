package com.itsuka.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 组织机构类型枚举
 *
 * @author itsuka
 */
@Getter
@AllArgsConstructor
public enum OrganizationType {
    /**
     * 公司
     */
    COMPANY("公司"),
    
    /**
     * 部门
     */
    DEPARTMENT("部门"),
    
    /**
     * 岗位
     */
    POSITION("岗位");
    
    /**
     * 描述
     */
    private final String description;
}