package com.itsuka.system.dto.organization;

import lombok.Data;

/**
 * 组织机构查询条件
 *
 * @author itsuka
 */
@Data
public class OrganizationQueryCriteria {
    /**
     * 组织名称
     */
    private String name;

    /**
     * 组织编码
     */
    private String code;

    /**
     * 组织类型：COMPANY-公司，DEPARTMENT-部门，POSITION-岗位
     */
    private String type;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
}