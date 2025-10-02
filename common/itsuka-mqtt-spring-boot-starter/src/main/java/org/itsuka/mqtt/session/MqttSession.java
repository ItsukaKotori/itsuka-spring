package org.itsuka.mqtt.session;

import io.netty.channel.Channel;
import lombok.Data;
import org.itsuka.mqtt.protocol.MqttQoS;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * MQTT会话
 * 
 * @author itsuka
 */
@Data
public class MqttSession {
    
    /**
     * 客户端ID
     */
    private String clientId;
    
    /**
     * 通道
     */
    private Channel channel;
    
    /**
     * 是否清除会话
     */
    private boolean cleanSession;
    
    /**
     * 会话创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;
    
    /**
     * 订阅主题集合 (主题 -> QoS)
     */
    private Map<String, MqttQoS> subscriptions;
    
    /**
     * 待发送的消息队列
     */
    private Set<String> pendingMessages;
    
    /**
     * 会话属性
     */
    private Map<String, Object> attributes;
    
    public MqttSession(String clientId, Channel channel, boolean cleanSession) {
        this.clientId = clientId;
        this.channel = channel;
        this.cleanSession = cleanSession;
        this.createTime = LocalDateTime.now();
        this.lastActiveTime = LocalDateTime.now();
        this.subscriptions = new ConcurrentHashMap<>();
        this.pendingMessages = new CopyOnWriteArraySet<>();
        this.attributes = new ConcurrentHashMap<>();
    }
    
    /**
     * 更新最后活跃时间
     */
    public void updateLastActiveTime() {
        this.lastActiveTime = LocalDateTime.now();
    }
    
    /**
     * 添加订阅
     */
    public void addSubscription(String topic, MqttQoS qos) {
        subscriptions.put(topic, qos);
    }
    
    /**
     * 移除订阅
     */
    public void removeSubscription(String topic) {
        subscriptions.remove(topic);
    }
    
    /**
     * 检查是否订阅了主题
     */
    public boolean isSubscribed(String topic) {
        return subscriptions.containsKey(topic);
    }
    
    /**
     * 获取订阅的QoS
     */
    public MqttQoS getSubscriptionQoS(String topic) {
        return subscriptions.get(topic);
    }
    
    /**
     * 添加待发送消息
     */
    public void addPendingMessage(String messageId) {
        pendingMessages.add(messageId);
    }
    
    /**
     * 移除待发送消息
     */
    public void removePendingMessage(String messageId) {
        pendingMessages.remove(messageId);
    }
    
    /**
     * 设置会话属性
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    /**
     * 获取会话属性
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    /**
     * 移除会话属性
     */
    public void removeAttribute(String key) {
        attributes.remove(key);
    }
    
    /**
     * 检查会话是否活跃
     */
    public boolean isActive() {
        return channel != null && channel.isActive();
    }
    
    /**
     * 获取客户端地址
     */
    public String getClientAddress() {
        return channel != null ? channel.remoteAddress().toString() : "unknown";
    }
}