package org.itsuka.mqtt.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * MQTT UNSUBSCRIBE消息有效载荷
 * 
 * @author itsuka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttUnsubscribePayload {
    
    /**
     * 要取消订阅的主题列表
     */
    private List<String> topics;
    
    /**
     * 添加主题
     */
    public void addTopic(String topic) {
        if (topics == null) {
            topics = new ArrayList<>();
        }
        topics.add(topic);
    }
}
