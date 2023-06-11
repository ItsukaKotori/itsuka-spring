package com.itsuka.system.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import lombok.Data;

/**
 * UserQueryCriteria
 *
 * @author pc
 */
@Data
@Schema(name = "用户查询实体")
public class UserQueryCriteria {
    @Schema(name = "用户名称")
    private String username;

    @Schema(name = "用户昵称")
    private String nickname;

    @Schema(name = "是否启用")
    private Boolean enable;
}
