package org.itsuka.mqtt.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT消息基类
 * 
 * @author itsuka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttMessage {
    
    /**
     * 固定头部
     */
    private MqttFixedHeader fixedHeader;
    
    /**
     * 可变头部
     */
    private Object variableHeader;
    
    /**
     * 有效载荷
     */
    private Object payload;
    
    /**
     * 构造方法 - 仅固定头部
     */
    public MqttMessage(MqttFixedHeader fixedHeader) {
        this.fixedHeader = fixedHeader;
    }
    
    /**
     * 构造方法 - 固定头部和可变头部
     */
    public MqttMessage(MqttFixedHeader fixedHeader, Object variableHeader) {
        this.fixedHeader = fixedHeader;
        this.variableHeader = variableHeader;
    }
    
    /**
     * 获取消息类型
     */
    public MqttMessageType getMessageType() {
        return fixedHeader != null ? fixedHeader.getMessageType() : null;
    }
    
    /**
     * 获取QoS等级
     */
    public MqttQoS getQosLevel() {
        return fixedHeader != null ? fixedHeader.getQosLevel() : null;
    }
    
    /**
     * 是否为重复消息
     */
    public boolean isDup() {
        return fixedHeader != null && fixedHeader.isDup();
    }
    
    /**
     * 是否为保留消息
     */
    public boolean isRetain() {
        return fixedHeader != null && fixedHeader.isRetain();
    }
}