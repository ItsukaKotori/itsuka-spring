package org.itsuka.mqtt.protocol;

/**
 * MQTT消息类型枚举
 * 
 * @author itsuka
 */
public enum MqttMessageType {
    
    CONNECT(1, "连接请求"),
    CONNACK(2, "连接确认"),
    PUBLISH(3, "发布消息"),
    PUBACK(4, "发布确认"),
    PUBREC(5, "发布收到"),
    PUBREL(6, "发布释放"),
    PUBCOMP(7, "发布完成"),
    SUBSCRIBE(8, "订阅请求"),
    SUBACK(9, "订阅确认"),
    UNSUBSCRIBE(10, "取消订阅"),
    UNSUBACK(11, "取消订阅确认"),
    PINGREQ(12, "心跳请求"),
    PINGRESP(13, "心跳响应"),
    DISCONNECT(14, "断开连接"),
    RESERVED(15, "保留");
    
    private final int value;
    private final String description;
    
    MqttMessageType(int value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public int getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static MqttMessageType valueOf(int value) {
        for (MqttMessageType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown MQTT message type: " + value);
    }
}