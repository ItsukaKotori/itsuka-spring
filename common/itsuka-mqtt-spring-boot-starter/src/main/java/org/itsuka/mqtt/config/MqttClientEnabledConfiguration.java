package org.itsuka.mqtt.config;

import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.client.MqttClient;
import org.itsuka.mqtt.client.MqttClientProperties;
import org.itsuka.mqtt.event.MqttEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通过注解启用 MQTT 客户端的配置。
 */
@Slf4j
@Configuration
@ConditionalOnClass(MqttClient.class)
@EnableConfigurationProperties(MqttClientProperties.class)
public class MqttClientEnabledConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MqttEventPublisher mqttEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        log.info("[Enable] Creating MQTT event publisher");
        return new MqttEventPublisher(applicationEventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public MqttClient mqttClient(MqttClientProperties properties,
                                 MqttEventPublisher eventPublisher) {
        log.info("[Enable] Creating MQTT client with properties: {}", properties);
        return new MqttClient(properties, eventPublisher);
    }
}