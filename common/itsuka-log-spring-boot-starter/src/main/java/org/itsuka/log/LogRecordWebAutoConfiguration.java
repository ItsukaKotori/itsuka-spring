package org.itsuka.log;

import org.itsuka.log.properties.LogRecordProperties;
import org.itsuka.log.spi.LogRequestResolver;
import org.itsuka.log.support.JacksonLogParameterSerializer;
import org.itsuka.log.support.LogParameterSerializer;
import org.itsuka.log.support.ServletLogRequestResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registers the servlet request resolver (and the Jackson parameter serializer) for servlet web
 * applications.
 *
 * <p>Runs before the core auto-configuration so its SPI implementations win over the no-op
 * defaults.</p>
 */
@AutoConfiguration(before = LogRecordAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.web.context.request.RequestContextHolder")
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnProperty(prefix = "itsuka.log", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LogRecordProperties.class)
public class LogRecordWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    LogRequestResolver servletLogRequestResolver(LogRecordProperties properties) {
        return new ServletLogRequestResolver(properties);
    }

    /**
     * Jackson-backed parameter serialization, active only when Jackson 3 is present.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "tools.jackson.databind.json.JsonMapper")
    static class JacksonConfiguration {

        @Bean
        @ConditionalOnMissingBean
        LogParameterSerializer jacksonLogParameterSerializer(JsonMapper mapper,
                LogRecordProperties properties) {
            return new JacksonLogParameterSerializer(mapper, properties);
        }
    }
}
