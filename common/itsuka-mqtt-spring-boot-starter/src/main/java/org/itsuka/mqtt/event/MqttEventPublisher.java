package org.itsuka.mqtt.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * MQTT事件发布器
 * 
 * @author itsuka
 */
@Slf4j
@Component
public class MqttEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public MqttEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * 发布MQTT消息事件
     */
    public void publishMessageEvent(MqttMessageEvent event) {
        try {
            eventPublisher.publishEvent(event);
            log.debug("Published MQTT message event: {} from {}", 
                    event.getMessageType(), event.getClientAddress());
        } catch (Exception e) {
            log.error("Failed to publish MQTT message event", e);
        }
    }
    
    /**
     * 发布连接事件
     */
    public void publishConnectionEvent(MqttConnectionEvent event) {
        try {
            eventPublisher.publishEvent(event);
            log.debug("Published MQTT connection event: {} for {}", 
                    event.getEventType(), event.getClientAddress());
        } catch (Exception e) {
            log.error("Failed to publish MQTT connection event", e);
        }
    }
}