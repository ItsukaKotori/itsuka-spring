package com.itsuka.system.dto.organization;

import lombok.Data;

import java.util.List;

/**
 * 组织机构树形结构DTO
 *
 * @author itsuka
 */
@Data
public class OrganizationTreeDto {
    /**
     * 主键
     */
    private Long id;

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
     * 父级ID，0表示顶级
     */
    private Long parentId;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 负责人ID
     */
    private Long leaderId;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 子节点列表
     */
    private List<OrganizationTreeDto> children;
}