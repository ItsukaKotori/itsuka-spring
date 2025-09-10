package org.itsuka.dict.properties;

import org.itsuka.dict.enums.DictCacheType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "itsuka.dict")
public class DictProperties {
    /**
     * 是否启用
     */
    private boolean enable;

    /**
     * 字典缓存类型
     */
    private DictCacheType type;

    /**
     * 字典加载类型
     */
    private boolean autoLoad;

    /**
     * 数据库类型，可选值：mysql, oracle, postgresql，不配置时自动根据驱动类名或URL识别
     */
    private String databaseType;

    /**
     * 自定义数据库方言类的全限定名，优先级高于databaseType
     * 设置后将使用该类的实例作为数据库方言
     */
    private String dialectClass;

    /**
     * 数据库属性
     */
    private DatasourceProperties datasource;


    @Data
    public static class DatasourceProperties {
        /**
         * 数据库连接信息
         */
        private String url;

        /**
         * 数据库用户名
         */
        private String username;

        /**
         * 数据库密码
         */
        private String password;

        /**
         * 数据库驱动类名
         */
        private String driverClassName;
    }
}
