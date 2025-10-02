package org.itsuka.mqtt.topic;

import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.protocol.MqttQoS;
import org.itsuka.mqtt.session.MqttSession;
import org.itsuka.mqtt.session.MqttSessionManager;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * MQTT主题管理器
 * 
 * @author itsuka
 */
@Slf4j
@Component
public class MqttTopicManager {
    
    private final MqttSessionManager sessionManager;
    
    /**
     * 主题订阅映射 (topic -> Set<clientId>)
     */
    private final Map<String, Set<String>> topicSubscriptions = new ConcurrentHashMap<>();
    
    /**
     * 通配符主题订阅映射 (wildcardTopic -> Set<clientId>)
     */
    private final Map<String, Set<String>> wildcardSubscriptions = new ConcurrentHashMap<>();
    
    public MqttTopicManager(MqttSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    /**
     * 添加订阅
     */
    public void addSubscription(String clientId, String topic, MqttQoS qos) {
        log.debug("Adding subscription for client {}: {} (QoS {})", clientId, topic, qos.getValue());
        
        // 更新会话中的订阅信息
        sessionManager.addSubscription(clientId, topic, qos);
        
        // 更新主题订阅映射
        if (isWildcardTopic(topic)) {
            wildcardSubscriptions.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet()).add(clientId);
        } else {
            topicSubscriptions.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet()).add(clientId);
        }
        
        log.info("Client {} subscribed to topic: {} with QoS {}", clientId, topic, qos.getValue());
    }
    
    /**
     * 移除订阅
     */
    public void removeSubscription(String clientId, String topic) {
        log.debug("Removing subscription for client {}: {}", clientId, topic);
        
        // 更新会话中的订阅信息
        sessionManager.removeSubscription(clientId, topic);
        
        // 更新主题订阅映射
        if (isWildcardTopic(topic)) {
            Set<String> subscribers = wildcardSubscriptions.get(topic);
            if (subscribers != null) {
                subscribers.remove(clientId);
                if (subscribers.isEmpty()) {
                    wildcardSubscriptions.remove(topic);
                }
            }
        } else {
            Set<String> subscribers = topicSubscriptions.get(topic);
            if (subscribers != null) {
                subscribers.remove(clientId);
                if (subscribers.isEmpty()) {
                    topicSubscriptions.remove(topic);
                }
            }
        }
        
        log.info("Client {} unsubscribed from topic: {}", clientId, topic);
    }
    
    /**
     * 移除客户端的所有订阅
     */
    public void removeAllSubscriptions(String clientId) {
        log.debug("Removing all subscriptions for client: {}", clientId);
        
        // 从普通主题订阅中移除
        topicSubscriptions.entrySet().removeIf(entry -> {
            entry.getValue().remove(clientId);
            return entry.getValue().isEmpty();
        });
        
        // 从通配符主题订阅中移除
        wildcardSubscriptions.entrySet().removeIf(entry -> {
            entry.getValue().remove(clientId);
            return entry.getValue().isEmpty();
        });
        
        log.info("Removed all subscriptions for client: {}", clientId);
    }
    
    /**
     * 获取订阅了指定主题的客户端
     */
    public Set<String> getSubscribers(String topic) {
        Set<String> subscribers = new HashSet<>();
        
        // 精确匹配的订阅者
        Set<String> exactSubscribers = topicSubscriptions.get(topic);
        if (exactSubscribers != null) {
            subscribers.addAll(exactSubscribers);
        }
        
        // 通配符匹配的订阅者
        for (Map.Entry<String, Set<String>> entry : wildcardSubscriptions.entrySet()) {
            String wildcardTopic = entry.getKey();
            if (matchesTopic(topic, wildcardTopic)) {
                subscribers.addAll(entry.getValue());
            }
        }
        
        return subscribers;
    }
    
    /**
     * 获取订阅了指定主题的会话
     */
    public List<MqttSession> getSubscribedSessions(String topic) {
        Set<String> subscriberIds = getSubscribers(topic);
        List<MqttSession> sessions = new ArrayList<>();
        
        for (String clientId : subscriberIds) {
            MqttSession session = sessionManager.getSession(clientId);
            if (session != null && session.isActive()) {
                sessions.add(session);
            }
        }
        
        return sessions;
    }
    
    /**
     * 检查主题是否包含通配符
     */
    private boolean isWildcardTopic(String topic) {
        return topic.contains("+") || topic.contains("#");
    }
    
    /**
     * 检查主题是否匹配通配符模式
     */
    private boolean matchesTopic(String topic, String wildcardTopic) {
        // 将MQTT通配符转换为正则表达式
        String regex = wildcardTopic
                .replace("+", "[^/]+")  // + 匹配单层
                .replace("#", ".*");    // # 匹配多层
        
        // 处理结尾的 # 通配符
        if (wildcardTopic.endsWith("#")) {
            regex = regex.substring(0, regex.length() - 2) + ".*";
        }
        
        return Pattern.matches(regex, topic);
    }
    
    /**
     * 获取所有订阅的主题
     */
    public Set<String> getAllTopics() {
        Set<String> allTopics = new HashSet<>();
        allTopics.addAll(topicSubscriptions.keySet());
        allTopics.addAll(wildcardSubscriptions.keySet());
        return allTopics;
    }
    
    /**
     * 获取指定客户端的订阅主题
     */
    public Set<String> getClientSubscriptions(String clientId) {
        Set<String> clientTopics = new HashSet<>();
        
        // 检查普通主题订阅
        for (Map.Entry<String, Set<String>> entry : topicSubscriptions.entrySet()) {
            if (entry.getValue().contains(clientId)) {
                clientTopics.add(entry.getKey());
            }
        }
        
        // 检查通配符主题订阅
        for (Map.Entry<String, Set<String>> entry : wildcardSubscriptions.entrySet()) {
            if (entry.getValue().contains(clientId)) {
                clientTopics.add(entry.getKey());
            }
        }
        
        return clientTopics;
    }
    
    /**
     * 获取订阅统计信息
     */
    public TopicStats getTopicStats() {
        return new TopicStats(
                topicSubscriptions.size() + wildcardSubscriptions.size(),
                topicSubscriptions.values().stream().mapToInt(Set::size).sum() +
                wildcardSubscriptions.values().stream().mapToInt(Set::size).sum()
        );
    }
    
    /**
     * 主题统计信息
     */
    public static class TopicStats {
        private final int totalTopics;
        private final int totalSubscriptions;
        
        public TopicStats(int totalTopics, int totalSubscriptions) {
            this.totalTopics = totalTopics;
            this.totalSubscriptions = totalSubscriptions;
        }
        
        public int getTotalTopics() { return totalTopics; }
        public int getTotalSubscriptions() { return totalSubscriptions; }
    }
}