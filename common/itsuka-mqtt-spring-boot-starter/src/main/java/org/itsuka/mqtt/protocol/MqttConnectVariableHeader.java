package org.itsuka.mqtt.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT CONNECT消息可变头部
 * 
 * @author itsuka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttConnectVariableHeader {
    
    /**
     * 协议名称
     */
    private String protocolName;
    
    /**
     * 协议版本
     */
    private int protocolVersion;
    
    /**
     * 连接标志
     */
    private MqttConnectFlags connectFlags;
    
    /**
     * 保持连接时间（秒）
     */
    private int keepAlive;
}
