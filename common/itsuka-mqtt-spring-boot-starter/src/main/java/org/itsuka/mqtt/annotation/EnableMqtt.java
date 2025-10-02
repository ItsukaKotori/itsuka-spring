package org.itsuka.mqtt.annotation;

import org.itsuka.mqtt.config.MqttClientEnabledConfiguration;
import org.itsuka.mqtt.config.MqttServerEnabledConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 同时启用 MQTT 服务器与客户端的注解。
 */
@Target(TYPE)
@Retention(RUNTIME)
@Documented
@Import({MqttServerEnabledConfiguration.class, MqttClientEnabledConfiguration.class})
public @interface EnableMqtt {
}