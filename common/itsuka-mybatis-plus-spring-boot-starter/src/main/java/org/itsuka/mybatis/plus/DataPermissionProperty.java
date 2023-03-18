package org.itsuka.mybatis.plus;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 数据权限相关属性
 *
 * @author Itsuka
 * @date 2023/3/5
 */
@ConfigurationProperties(prefix = "itsuka.mybatis-plus.data-permission")
@Data
public class DataPermissionProperty {
    /**
     * 无需进行数据权限校验的表
     */
    private String[] excludeTables;

    /**
     * 用户表名称
     */
    private String userTableName = "user";

    /**
     * 是否必须包含create_by
     */
    private boolean mustContainCreateBy;

    /**
     * 创建人字段名称
     */
    private String createByField = "create_by";
}
