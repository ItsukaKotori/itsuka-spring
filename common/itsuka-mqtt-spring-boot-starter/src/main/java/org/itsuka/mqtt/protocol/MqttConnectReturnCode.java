package org.itsuka.mqtt.protocol;

/**
 * MQTT连接返回码
 * 
 * @author itsuka
 */
public enum MqttConnectReturnCode {
    
    /**
     * 连接已接受
     */
    CONNECTION_ACCEPTED(0x00),
    
    /**
     * 不可接受的协议版本
     */
    UNACCEPTABLE_PROTOCOL_VERSION(0x01),
    
    /**
     * 标识符被拒绝
     */
    IDENTIFIER_REJECTED(0x02),
    
    /**
     * 服务器不可用
     */
    SERVER_UNAVAILABLE(0x03),
    
    /**
     * 错误的用户名或密码
     */
    BAD_USERNAME_OR_PASSWORD(0x04),
    
    /**
     * 未授权
     */
    NOT_AUTHORIZED(0x05);
    
    private final int value;
    
    MqttConnectReturnCode(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public static MqttConnectReturnCode valueOf(int value) {
        for (MqttConnectReturnCode code : values()) {
            if (code.value == value) {
                return code;
            }
        }
        throw new IllegalArgumentException("Unknown return code: " + value);
    }
}
