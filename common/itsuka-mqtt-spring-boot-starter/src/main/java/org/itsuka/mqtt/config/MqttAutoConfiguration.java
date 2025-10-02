package org.itsuka.mqtt.config;

import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.client.MqttClient;
import org.itsuka.mqtt.client.MqttClientProperties;
import org.itsuka.mqtt.event.MqttEventPublisher;
import org.itsuka.mqtt.server.MqttServer;
import org.itsuka.mqtt.server.MqttServerProperties;
import org.itsuka.mqtt.session.MqttSessionManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * MQTT自动配置类
 * 
 * @author itsuka
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({MqttServer.class, MqttClient.class})
@EnableConfigurationProperties({MqttServerProperties.class, MqttClientProperties.class})
@ComponentScan(basePackages = "org.itsuka.mqtt")
public class MqttAutoConfiguration {
    
    /**
     * MQTT会话管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public MqttSessionManager mqttSessionManager() {
        log.info("Creating MQTT session manager");
        return new MqttSessionManager();
    }
    
    /**
     * MQTT事件发布器
     */
    @Bean
    @ConditionalOnMissingBean
    public MqttEventPublisher mqttEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        log.info("Creating MQTT event publisher");
        return new MqttEventPublisher(applicationEventPublisher);
    }
    
    /**
     * MQTT服务器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "itsuka.mqtt.server", name = "enabled", havingValue = "true", matchIfMissing = false)
    public MqttServer mqttServer(MqttServerProperties properties, 
                                MqttSessionManager sessionManager,
                                MqttEventPublisher eventPublisher) {
        log.info("Creating MQTT server with properties: {}", properties);
        return new MqttServer(properties, sessionManager, eventPublisher);
    }
    
    /**
     * MQTT客户端
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "itsuka.mqtt.client", name = "enabled", havingValue = "true", matchIfMissing = false)
    public MqttClient mqttClient(MqttClientProperties properties,
                                MqttEventPublisher eventPublisher) {
        log.info("Creating MQTT client with properties: {}", properties);
        return new MqttClient(properties, eventPublisher);
    }
}