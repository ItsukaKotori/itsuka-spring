package org.itsuka.mqtt.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * MQTT SUBSCRIBE消息有效载荷
 * 
 * @author itsuka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttSubscribePayload {
    
    /**
     * 主题订阅列表
     */
    private List<MqttTopicSubscription> topicSubscriptions;
    
    /**
     * 添加主题订阅
     */
    public void addSubscription(String topic, MqttQoS qos) {
        if (topicSubscriptions == null) {
            topicSubscriptions = new ArrayList<>();
        }
        topicSubscriptions.add(new MqttTopicSubscription(topic, qos));
    }
}
