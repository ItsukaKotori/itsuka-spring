package com.itsuka.system.dto.resource;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 资源创建DTO
 *
 * @author itsuka
 */
@Data
public class ResourceCreateDto {
    /**
     * 资源名称
     */
    @NotNull(message = "资源名称不能为空!")
    private String name;

    /**
     * 资源编码
     */
    @NotNull(message = "资源编码不能为空!")
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