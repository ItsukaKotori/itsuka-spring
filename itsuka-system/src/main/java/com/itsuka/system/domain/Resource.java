package com.itsuka.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.itsuka.mybatis.plus.domain.BaseDomain;

/**
 * 菜单表
 *
 * @author  itsuka
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value ="resource")
@Data
public class Resource extends BaseDomain {
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 菜单编码
     */
    private String code;

    /**
     * 资源类型
     */
    private String type;

    /**
     * 前端组件
     */
    private String component;

    /**
     * 路由路径
     */
    private String path;

    /**
     * 前端图标
     */
    private String icon;

    /**
     * 父级ID
     */
    private Long pid;
}