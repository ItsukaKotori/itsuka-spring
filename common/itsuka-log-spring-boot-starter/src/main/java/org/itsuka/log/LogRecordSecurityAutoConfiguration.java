package org.itsuka.log;

import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.spi.LogOperatorProvider;
import org.itsuka.log.support.SecurityLogOperatorProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Registers the Spring Security backed operator provider when Spring Security is on the classpath.
 *
 * <p>Runs before the core auto-configuration so its provider wins over the default one.</p>
 */
@AutoConfiguration(before = LogRecordAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.security.core.context.SecurityContextHolder")
@ConditionalOnProperty(prefix = "itsuka.log", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LogRecordProperties.class)
public class LogRecordSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    LogOperatorProvider securityLogOperatorProvider(LogRecordProperties properties) {
        return new SecurityLogOperatorProvider(properties);
    }
}
