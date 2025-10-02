package org.itsuka.mqtt.event;

import io.netty.channel.Channel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.itsuka.mqtt.protocol.MqttMessage;
import org.itsuka.mqtt.protocol.MqttMessageType;
import org.springframework.context.ApplicationEvent;

/**
 * MQTT消息事件
 * 
 * @author itsuka
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class MqttMessageEvent extends ApplicationEvent {
    
    /**
     * 通道
     */
    private Channel channel;
    
    /**
     * MQTT消息
     */
    private MqttMessage message;
    
    public MqttMessageEvent(Channel channel, MqttMessage message) {
        super(message);
        this.channel = channel;
        this.message = message;
    }
    
    /**
     * 获取客户端地址
     */
    public String getClientAddress() {
        return channel != null ? channel.remoteAddress().toString() : "unknown";
    }
    
    /**
     * 获取消息类型（枚举）
     */
    public MqttMessageType getMessageType() {
        return message != null ? message.getMessageType() : null;
    }
}