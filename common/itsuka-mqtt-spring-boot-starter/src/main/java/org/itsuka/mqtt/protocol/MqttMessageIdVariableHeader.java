package org.itsuka.mqtt.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT消息ID可变头部（用于PUBACK、PUBREC、PUBREL、PUBCOMP、UNSUBACK）
 * 
 * @author itsuka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttMessageIdVariableHeader {
    
    /**
     * 消息ID
     */
    private int messageId;
}
