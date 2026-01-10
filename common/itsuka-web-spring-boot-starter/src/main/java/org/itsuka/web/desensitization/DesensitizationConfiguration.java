package org.itsuka.web.desensitization;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 脱敏配置类
 * 提供脱敏功能的自动配置
 *
 * @author Itsuka
 * @since 2023/12/10
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "itsuka.web.desensitization", value = "enabled", havingValue = "true", matchIfMissing = true)
public class DesensitizationConfiguration {
    // 配置类本身不需要提供Bean，脱敏功能通过注解和序列化器自动生效
}