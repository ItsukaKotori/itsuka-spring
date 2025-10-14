package org.itsuka.mqtt.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT CONNECT消息有效载荷
 * 
 * @author itsuka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttConnectPayload {
    
    /**
     * 客户端ID
     */
    private String clientId;
    
    /**
     * 遗嘱主题
     */
    private String willTopic;
    
    /**
     * 遗嘱消息
     */
    private byte[] willMessage;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private byte[] password;
}
