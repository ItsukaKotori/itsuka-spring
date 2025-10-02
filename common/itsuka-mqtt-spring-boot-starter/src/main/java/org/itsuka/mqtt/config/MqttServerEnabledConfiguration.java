package org.itsuka.mqtt.config;

import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.event.MqttEventPublisher;
import org.itsuka.mqtt.server.MqttServer;
import org.itsuka.mqtt.server.MqttServerProperties;
import org.itsuka.mqtt.session.MqttSessionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通过注解启用 MQTT 服务器的配置。
 */
@Slf4j
@Configuration
@ConditionalOnClass(MqttServer.class)
@EnableConfigurationProperties(MqttServerProperties.class)
public class MqttServerEnabledConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MqttSessionManager mqttSessionManager() {
        log.info("[Enable] Creating MQTT session manager");
        return new MqttSessionManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public MqttEventPublisher mqttEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        log.info("[Enable] Creating MQTT event publisher");
        return new MqttEventPublisher(applicationEventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public MqttServer mqttServer(MqttServerProperties properties,
                                 MqttSessionManager sessionManager,
                                 MqttEventPublisher eventPublisher) {
        log.info("[Enable] Creating MQTT server with properties: {}", properties);
        return new MqttServer(properties, sessionManager, eventPublisher);
    }
}