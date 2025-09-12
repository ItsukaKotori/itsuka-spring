package com.itsuka.system.dto.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色查询条件
 *
 * @author itsuka
 */
@Data
@Schema(name = "角色查询实体")
public class RoleQueryCriteria {
    @Schema(name = "角色编码")
    private String code;

    @Schema(name = "角色名称")
    private String name;
}