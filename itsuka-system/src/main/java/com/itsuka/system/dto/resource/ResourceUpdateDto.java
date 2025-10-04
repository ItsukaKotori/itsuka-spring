package com.itsuka.system.dto.resource;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 资源更新DTO
 *
 * @author itsuka
 */
@Data
public class ResourceUpdateDto {
    /**
     * 资源ID
     */
    @NotNull(message = "资源ID不能为空!")
    private Long id;

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