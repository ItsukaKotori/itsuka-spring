package org.itsuka.web.desensitization;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 脱敏配置属性类
 * 用于配置脱敏功能的全局设置
 *
 * @author Itsuka
 * @since 2023/12/7
 */
@Data
@ConfigurationProperties(prefix = "itsuka.web.desensitization")
public class DesensitizationProperties {

    /**
     * 是否启用脱敏功能
     */
    private boolean enabled = true;

    /**
     * 默认替换字符
     */
    private char replacer = '*';

    /**
     * 默认保留前缀长度
     */
    private int defaultPrefixKeep = 0;

    /**
     * 默认保留后缀长度
     */
    private int defaultSuffixKeep = 0;

    /**
     * 是否对日志输出也进行脱敏
     */
    private boolean logDesensitization = false;

    /**
     * 是否对错误信息中的敏感数据进行脱敏
     */
    private boolean errorDesensitization = false;
}