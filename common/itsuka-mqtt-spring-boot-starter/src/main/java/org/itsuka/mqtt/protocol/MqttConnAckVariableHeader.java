package org.itsuka.mqtt.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT CONNACK消息可变头部
 * 
 * @author itsuka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttConnAckVariableHeader {
    
    /**
     * 会话存在标志
     */
    private boolean sessionPresent;
    
    /**
     * 连接返回码
     */
    private MqttConnectReturnCode returnCode;
}
