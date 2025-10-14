package org.itsuka.mqtt.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT主题订阅
 * 
 * @author itsuka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttTopicSubscription {
    
    /**
     * 主题过滤器
     */
    private String topicFilter;
    
    /**
     * QoS等级
     */
    private MqttQoS qos;
}
