package com.itsuka.security.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 访问控制配置属性
 *
 * @author itsuka
 */
@Data
@ConfigurationProperties(prefix = "itsuka.security.access-control")
public class AccessControlProperties {

    /**
     * 是否启用访问控制
     */
    private boolean enabled = true;

    /**
     * 白名单配置
     */
    private Whitelist whitelist = new Whitelist();

    /**
     * 黑名单配置
     */
    private Blacklist blacklist = new Blacklist();

    @Data
    public static class Whitelist {
        /**
         * 是否启用白名单
         */
        private boolean enabled = false;

        /**
         * 白名单路径列表，支持Ant风格路径匹配
         */
        private List<String> paths;

        /**
         * 白名单IP地址列表，支持CIDR格式
         */
        private List<String> ips;

        /**
         * 白名单User-Agent列表，支持通配符匹配
         */
        private List<String> userAgents;
    }

    @Data
    public static class Blacklist {
        /**
         * 是否启用黑名单
         */
        private boolean enabled = false;

        /**
         * 黑名单路径列表，支持Ant风格路径匹配
         */
        private List<String> paths;

        /**
         * 黑名单IP地址列表，支持CIDR格式
         */
        private List<String> ips;

        /**
         * 黑名单User-Agent列表，支持通配符匹配
         */
        private List<String> userAgents;
    }
}