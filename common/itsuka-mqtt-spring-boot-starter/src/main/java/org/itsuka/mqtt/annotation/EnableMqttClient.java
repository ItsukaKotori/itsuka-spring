package org.itsuka.mqtt.annotation;

import org.itsuka.mqtt.config.MqttClientEnabledConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 启用 MQTT 客户端的注解。
 * 使用此注解后无需在配置中显式设置 {@code itsuka.mqtt.client.enabled=true}。
 */
@Target(TYPE)
@Retention(RUNTIME)
@Documented
@Import(MqttClientEnabledConfiguration.class)
public @interface EnableMqttClient {
}