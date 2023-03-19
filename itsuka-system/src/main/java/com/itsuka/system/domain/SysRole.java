package com.itsuka.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.itsuka.mybatis.plus.domain.BaseDomain;

/**
 * 角色表
 * @author itsuka
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value ="sys_role")
@Data
public class SysRole extends BaseDomain {
    /**
     * 
     */
    @TableId
    private Long id;

    /**
     * 编码
     */
    private String code;

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;
}