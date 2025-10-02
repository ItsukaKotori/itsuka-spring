package org.itsuka.mqtt.event;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationEvent;

/**
 * MQTT连接事件
 * 
 * @author itsuka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttConnectionEvent extends ApplicationEvent {
    
    /**
     * 事件类型枚举
     */
    public enum EventType {
        CONNECTED("连接"),
        DISCONNECTED("断开连接"),
        AUTHENTICATION_FAILED("认证失败"),
        CONNECTION_LOST("连接丢失");
        
        private final String description;
        
        EventType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 通道
     */
    private Channel channel;
    
    /**
     * 客户端ID
     */
    private String clientId;
    
    /**
     * 事件类型
     */
    private EventType eventType;
    
    /**
     * 事件时间戳
     */
    private long timestamp;
    
    /**
     * 错误信息（可选）
     */
    private String errorMessage;
    
    public MqttConnectionEvent(Channel channel, String clientId, EventType eventType) {
        super(clientId);
        this.channel = channel;
        this.clientId = clientId;
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();
    }
    
    public MqttConnectionEvent(Channel channel, String clientId, EventType eventType, String errorMessage) {
        this(channel, clientId, eventType);
        this.errorMessage = errorMessage;
    }
    
    /**
     * 获取客户端地址
     */
    public String getClientAddress() {
        return channel != null ? channel.remoteAddress().toString() : "unknown";
    }
}