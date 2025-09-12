package com.itsuka.system.dto.resource;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 资源查询条件
 *
 * @author itsuka
 */
@Data
@Schema(name = "资源查询实体")
public class ResourceQueryCriteria {
    @Schema(name = "资源名称")
    private String name;

    @Schema(name = "资源编码")
    private String code;

    @Schema(name = "父级ID")
    private Long pid;
}