package org.itsuka.mqtt.event;

import io.netty.channel.Channel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * MQTT连接事件
 * 
 * @author itsuka
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
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
     * 错误信息（可选）
     */
    private String errorMessage;
    
    public MqttConnectionEvent(Channel channel, String clientId, EventType eventType) {
        super(clientId);
        this.channel = channel;
        this.clientId = clientId;
        this.eventType = eventType;
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