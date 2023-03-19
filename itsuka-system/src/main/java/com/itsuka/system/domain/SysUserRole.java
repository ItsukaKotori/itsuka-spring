package com.itsuka.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户角色关联表
 *
 * @author itsuka
 */
@TableName(value ="sys_user_role")
@Data
public class SysUserRole {
    /**
     * 用户ID
     */
    @TableId
    private Long userId;

    /**
     * 角色ID
     */
    @TableId
    private Long roleId;
}