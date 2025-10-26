package com.itsuka.system.dto.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 组织机构创建DTO
 *
 * @author itsuka
 */
@Data
public class OrganizationCreateDto {
    /**
     * 组织名称
     */
    @NotBlank(message = "组织名称不能为空")
    private String name;

    /**
     * 组织编码
     */
    @NotBlank(message = "组织编码不能为空")
    private String code;

    /**
     * 组织类型：COMPANY-公司，DEPARTMENT-部门，POSITION-岗位
     */
    @NotBlank(message = "组织类型不能为空")
    private String type;

    /**
     * 父级ID，0表示顶级
     */
    @NotNull(message = "父级ID不能为空")
    private Long parentId;

    /**
     * 排序号
     */
    private Integer sortOrder = 0;

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
    private Integer status = 1;
}