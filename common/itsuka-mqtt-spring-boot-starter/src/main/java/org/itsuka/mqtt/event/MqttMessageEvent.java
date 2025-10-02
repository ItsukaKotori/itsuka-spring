package org.itsuka.mqtt.event;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.itsuka.mqtt.protocol.MqttMessage;
import org.springframework.context.ApplicationEvent;

/**
 * MQTT消息事件
 * 
 * @author itsuka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttMessageEvent extends ApplicationEvent {
    
    /**
     * 通道
     */
    private Channel channel;
    
    /**
     * MQTT消息
     */
    private MqttMessage message;
    
    /**
     * 事件时间戳
     */
    private long timestamp;
    
    public MqttMessageEvent(Channel channel, MqttMessage message) {
        super(message);
        this.channel = channel;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 获取客户端地址
     */
    public String getClientAddress() {
        return channel != null ? channel.remoteAddress().toString() : "unknown";
    }
    
    /**
     * 获取消息类型
     */
    public String getMessageType() {
        return message != null ? message.getMessageType().name() : "unknown";
    }
}