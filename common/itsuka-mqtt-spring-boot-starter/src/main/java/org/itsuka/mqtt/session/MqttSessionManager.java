package org.itsuka.mqtt.session;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.protocol.MqttQoS;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT会话管理器
 * 
 * @author itsuka
 */
@Slf4j
@Component
public class MqttSessionManager {
    
    /**
     * 会话存储 (clientId -> MqttSession)
     */
    private final Map<String, MqttSession> sessions = new ConcurrentHashMap<>();
    
    /**
     * 通道到客户端ID的映射 (channel -> clientId)
     */
    private final Map<Channel, String> channelToClientId = new ConcurrentHashMap<>();
    
    /**
     * 创建会话
     */
    public MqttSession createSession(Channel channel, String clientId) {
        return createSession(channel, clientId, true);
    }
    
    /**
     * 创建会话
     */
    public MqttSession createSession(Channel channel, String clientId, boolean cleanSession) {
        log.info("Creating MQTT session for client: {} from {}", clientId, channel.remoteAddress());
        
        // 如果是清除会话，先移除旧会话
        if (cleanSession) {
            removeSession(clientId);
        }
        
        // 创建新会话
        MqttSession session = new MqttSession(clientId, channel, cleanSession);
        sessions.put(clientId, session);
        channelToClientId.put(channel, clientId);
        
        log.debug("MQTT session created for client: {}", clientId);
        return session;
    }
    
    /**
     * 获取会话
     */
    public MqttSession getSession(String clientId) {
        return sessions.get(clientId);
    }
    
    /**
     * 根据通道获取会话
     */
    public MqttSession getSession(Channel channel) {
        String clientId = channelToClientId.get(channel);
        return clientId != null ? sessions.get(clientId) : null;
    }
    
    /**
     * 移除会话（根据客户端ID）
     */
    public void removeSession(String clientId) {
        MqttSession session = sessions.remove(clientId);
        if (session != null) {
            channelToClientId.remove(session.getChannel());
            log.info("Removed MQTT session for client: {}", clientId);
        }
    }
    
    /**
     * 移除会话（根据通道）
     */
    public void removeSession(Channel channel) {
        String clientId = channelToClientId.remove(channel);
        if (clientId != null) {
            sessions.remove(clientId);
            log.info("Removed MQTT session for client: {} from channel", clientId);
        }
    }
    
    /**
     * 检查会话是否存在
     */
    public boolean hasSession(String clientId) {
        return sessions.containsKey(clientId);
    }
    
    /**
     * 获取所有会话
     */
    public Collection<MqttSession> getAllSessions() {
        return sessions.values();
    }
    
    /**
     * 获取活跃会话数量
     */
    public int getActiveSessionCount() {
        return (int) sessions.values().stream()
                .filter(MqttSession::isActive)
                .count();
    }
    
    /**
     * 获取总会话数量
     */
    public int getTotalSessionCount() {
        return sessions.size();
    }
    
    /**
     * 添加订阅
     */
    public void addSubscription(String clientId, String topic, MqttQoS qos) {
        MqttSession session = sessions.get(clientId);
        if (session != null) {
            session.addSubscription(topic, qos);
            log.debug("Added subscription for client {}: {} (QoS {})", clientId, topic, qos.getValue());
        }
    }
    
    /**
     * 移除订阅
     */
    public void removeSubscription(String clientId, String topic) {
        MqttSession session = sessions.get(clientId);
        if (session != null) {
            session.removeSubscription(topic);
            log.debug("Removed subscription for client {}: {}", clientId, topic);
        }
    }
    
    /**
     * 获取订阅了指定主题的会话
     */
    public Collection<MqttSession> getSubscribedSessions(String topic) {
        return sessions.values().stream()
                .filter(session -> session.isSubscribed(topic))
                .toList();
    }
    
    /**
     * 清理非活跃会话
     */
    public void cleanupInactiveSessions() {
        sessions.entrySet().removeIf(entry -> {
            MqttSession session = entry.getValue();
            if (!session.isActive() && session.isCleanSession()) {
                channelToClientId.remove(session.getChannel());
                log.debug("Cleaned up inactive session for client: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 更新会话活跃时间
     */
    public void updateSessionActivity(String clientId) {
        MqttSession session = sessions.get(clientId);
        if (session != null) {
            session.updateLastActiveTime();
        }
    }
    
    /**
     * 更新会话活跃时间（根据通道）
     */
    public void updateSessionActivity(Channel channel) {
        String clientId = channelToClientId.get(channel);
        if (clientId != null) {
            updateSessionActivity(clientId);
        }
    }
}