package org.itsuka.flyway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Flyway 多路径迁移配置属性。
 *
 * @author Itsuka
 */
@ConfigurationProperties("itsuka.flyway")
public class ItsukaFlywayProperties {

    /**
     * 是否启用多路径迁移
     */
    private boolean enabled = true;

    /**
     * 是否切回 Spring Boot 原生单路径 Flyway
     */
    private boolean nativeEnabled = false;

    /**
     * source 路径不存在时是否启动失败
     */
    private boolean failOnMissingLocations = true;

    /**
     * 迁移路径列表，每个 source 对应一条独立的版本流与历史表
     */
    private List<Source> sources = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isNativeEnabled() {
        return nativeEnabled;
    }

    public void setNativeEnabled(boolean nativeEnabled) {
        this.nativeEnabled = nativeEnabled;
    }

    public boolean isFailOnMissingLocations() {
        return failOnMissingLocations;
    }

    public void setFailOnMissingLocations(boolean failOnMissingLocations) {
        this.failOnMissingLocations = failOnMissingLocations;
    }

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    /**
     * 单个迁移路径配置。
     */
    public static class Source {

        /**
         * source 名，唯一，用于派生 Bean 名与历史表名
         */
        private String name;

        /**
         * Flyway location，例如 {@code classpath:db/migration/order}
         */
        private String location;

        /**
         * 历史表名，未配置时由规范化后的 name 派生
         */
        private String table;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        /**
         * 返回将 {@code -} 替换为 {@code _} 后的 source 名。
         *
         * @return 规范化 name，name 为 {@code null} 时返回 {@code null}
         */
        public String normalizedName() {
            return (name != null) ? name.replace('-', '_') : null;
        }

        /**
         * 返回历史表名：显式配置优先，否则由规范化后的 name 派生。
         *
         * @return 该 source 使用的历史表名
         */
        public String resolvedTable() {
            return (table != null && !table.isBlank())
                ? table
                : "flyway_schema_history_" + normalizedName();
        }
    }
}
