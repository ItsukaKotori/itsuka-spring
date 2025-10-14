package org.itsuka.mqtt.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT PUBLISH消息可变头部
 * 
 * @author itsuka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttPublishVariableHeader {
    
    /**
     * 主题名称
     */
    private String topicName;
    
    /**
     * 消息ID（QoS > 0时需要）
     */
    private int messageId;
}
