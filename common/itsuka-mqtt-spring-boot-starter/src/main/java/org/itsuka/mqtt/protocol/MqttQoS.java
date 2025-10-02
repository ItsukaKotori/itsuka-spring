package org.itsuka.mqtt.protocol;

/**
 * MQTT QoS等级枚举
 * 
 * @author itsuka
 */
public enum MqttQoS {
    
    AT_MOST_ONCE(0, "最多一次"),
    AT_LEAST_ONCE(1, "至少一次"),
    EXACTLY_ONCE(2, "恰好一次"),
    RESERVED(3, "保留");
    
    private final int value;
    private final String description;
    
    MqttQoS(int value, String description) {
        this.value = value;
        this.description = description;
    }
    
    public int getValue() {
        return value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static MqttQoS valueOf(int value) {
        for (MqttQoS qos : values()) {
            if (qos.value == value) {
                return qos;
            }
        }
        throw new IllegalArgumentException("Unknown MQTT QoS: " + value);
    }
}