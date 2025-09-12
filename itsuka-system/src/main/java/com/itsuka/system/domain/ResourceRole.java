package com.itsuka.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 角色资源关联表
 *
 * @author itsuka
 */
@TableName(value ="resource_role")
@Data
@Accessors(chain = true)
public class ResourceRole {
    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 资源ID
     */
    private Long resourceId;
}