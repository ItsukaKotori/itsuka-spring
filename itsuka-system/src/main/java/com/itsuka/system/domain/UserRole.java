package com.itsuka.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户角色关联表
 *
 * @author itsuka
 */
@TableName(value ="user_role")
@Data
@Accessors(chain = true)
public class UserRole {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色ID
     */
    private Long roleId;
}