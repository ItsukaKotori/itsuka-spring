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
 * 优化的MQTT主题管理器 - 使用Trie结构提高通配符匹配性能
 * 
 * @author itsuka
 */
@Slf4j
@Component
public class MqttTopicManager {
    
    private final MqttSessionManager sessionManager;
    
    /**
     * 精确主题订阅映射 (topic -> Set<clientId>)
     */
    private final Map<String, Set<String>> topicSubscriptions = new ConcurrentHashMap<>();
    
    /**
     * 通配符主题Trie节点
     */
    private final TrieNode wildcardTrie = new TrieNode();
    
    /**
     * 客户端订阅缓存 (clientId -> Set<topic>)
     */
    private final Map<String, Set<String>> clientSubscriptions = new ConcurrentHashMap<>();
    
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
        
        // 更新客户端订阅缓存
        clientSubscriptions.computeIfAbsent(clientId, k -> ConcurrentHashMap.newKeySet()).add(topic);
        
        // 更新主题订阅映射
        if (isWildcardTopic(topic)) {
            addToTrie(topic, clientId);
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
        
        // 从客户端订阅缓存中移除
        Set<String> clientTopics = clientSubscriptions.get(clientId);
        if (clientTopics != null) {
            clientTopics.remove(topic);
            if (clientTopics.isEmpty()) {
                clientSubscriptions.remove(clientId);
            }
        }
        
        // 从主题订阅映射中移除
        if (isWildcardTopic(topic)) {
            removeFromTrie(topic, clientId);
        } else {
            removeFromMap(topicSubscriptions, topic, clientId);
        }
        
        log.info("Client {} unsubscribed from topic: {}", clientId, topic);
    }
    
    /**
     * 移除客户端的所有订阅
     */
    public void removeAllSubscriptions(String clientId) {
        log.debug("Removing all subscriptions for client: {}", clientId);
        
        Set<String> topics = clientSubscriptions.get(clientId);
        if (topics != null) {
            for (String topic : topics) {
                if (isWildcardTopic(topic)) {
                    removeFromTrie(topic, clientId);
                } else {
                    removeFromMap(topicSubscriptions, topic, clientId);
                }
            }
        }
        
        clientSubscriptions.remove(clientId);
        
        log.info("Removed all subscriptions for client: {}", clientId);
    }
    
    /**
     * 发布消息到订阅者
     */
    public void publishMessage(String topic, byte[] payload, MqttQoS qos, String publisherId) {
        Set<String> subscribers = getSubscribers(topic);
        
        if (subscribers.isEmpty()) {
            log.debug("No subscribers for topic: {}", topic);
            return;
        }
        
        // 排除发布者自己
        if (publisherId != null) {
            subscribers.remove(publisherId);
        }
        
        log.info("Publishing message to topic: {} with {} subscribers", topic, subscribers.size());
        
        for (String clientId : subscribers) {
            MqttSession session = sessionManager.getSession(clientId);
            if (session != null && session.isActive()) {
                // 这里应该发送消息给客户端
                // 实际实现中需要使用Netty channel发送PUBLISH消息
                log.debug("Sending message to client: {}", clientId);
            } else {
                log.warn("Session not found or inactive for client: {}", clientId);
            }
        }
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
        
        // 通配符匹配的订阅者 - 使用Trie优化
        subscribers.addAll(findWildcardSubscribers(topic));
        
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
     * 获取所有订阅的主题
     */
    public Set<String> getAllTopics() {
        Set<String> allTopics = new HashSet<>();
        allTopics.addAll(topicSubscriptions.keySet());
        allTopics.addAll(clientSubscriptions.values().stream().flatMap(Set::stream).toList());
        return allTopics;
    }
    
    /**
     * 获取指定客户端的订阅主题
     */
    public Set<String> getClientSubscriptions(String clientId) {
        Set<String> topics = clientSubscriptions.get(clientId);
        return topics != null ? new HashSet<>(topics) : Collections.emptySet();
    }
    
    /**
     * 获取订阅统计信息
     */
    public TopicStats getTopicStats() {
        int totalTopics = topicSubscriptions.size() + countWildcardTopics();
        int totalSubscriptions = topicSubscriptions.values().stream().mapToInt(Set::size).sum()
                                    + countWildcardSubscriptions();
        int totalClients = clientSubscriptions.size();
        
        return new TopicStats(totalTopics, totalSubscriptions, totalClients);
    }
    
    /**
     * 添加通配符订阅到Trie
     */
    private void addToTrie(String topic, String clientId) {
        String[] levels = topic.split("/");
        TrieNode node = wildcardTrie;
        
        for (String level : levels) {
            if ("#".equals(level)) {
                node.clients.computeIfAbsent(level, k -> ConcurrentHashMap.newKeySet()).add(clientId);
                break;
            } else if ("+".equals(level)) {
                node.clients.computeIfAbsent(level, k -> ConcurrentHashMap.newKeySet()).add(clientId);
                node = node.children.computeIfAbsent(level, k -> new TrieNode());
            } else {
                node = node.children.computeIfAbsent(level, k -> new TrieNode());
            }
        }
    }
    
    /**
     * 从Trie中移除通配符订阅
     */
    private void removeFromTrie(String topic, String clientId) {
        String[] levels = topic.split("/");
        removeRecursive(wildcardTrie, levels, 0, clientId);
    }
    
    /**
     * 递归移除Trie节点
     */
    private boolean removeRecursive(TrieNode node, String[] levels, int index, String clientId) {
        if (index >= levels.length) {
            return true;
        }
        
        String level = levels[index];
        
        if ("#".equals(level)) {
            Set<String> clients = node.clients.get("#");
            if (clients != null) {
                clients.remove(clientId);
                if (clients.isEmpty()) {
                    node.clients.remove("#");
                }
            }
            return true;
        }
        
        TrieNode child = node.children.get(level);
        if (child == null) {
            return true;
        }
        
        boolean shouldRemove = removeRecursive(child, levels, index + 1, clientId);
        
        // 清理客户端引用
        Set<String> clients = node.clients.get(level);
        if (clients != null) {
            clients.remove(clientId);
            if (clients.isEmpty()) {
                node.clients.remove(level);
            }
        }
        
        // 如果子节点没有客户端和子节点，则可以删除
        if (shouldRemove && child.clients.isEmpty() && child.children.isEmpty()) {
            node.children.remove(level);
        }
        
        return node.clients.isEmpty() && node.children.isEmpty();
    }
    
    /**
     * 使用Trie查找通配符订阅者
     */
    private Set<String> findWildcardSubscribers(String topic) {
        Set<String> subscribers = new HashSet<>();
        String[] levels = topic.split("/");
        findWildcardRecursive(wildcardTrie, levels, 0, subscribers);
        return subscribers;
    }
    
    /**
     * 递归查找通配符订阅者
     */
    private void findWildcardRecursive(TrieNode node, String[] levels, int index, Set<String> subscribers) {
        // 检查当前节点的客户端
        for (Set<String> clients : node.clients.values()) {
            subscribers.addAll(clients);
        }
        
        if (index >= levels.length) {
            return;
        }
        
        String currentLevel = levels[index];
        
        // 精确匹配
        TrieNode exactChild = node.children.get(currentLevel);
        if (exactChild != null) {
            findWildcardRecursive(exactChild, levels, index + 1, subscribers);
        }
        
        // 单层通配符匹配
        TrieNode plusChild = node.children.get("+");
        if (plusChild != null) {
            findWildcardRecursive(plusChild, levels, index + 1, subscribers);
        }
        
        // 多层通配符匹配
        Set<String> hashClients = node.clients.get("#");
        if (hashClients != null) {
            subscribers.addAll(hashClients);
        }
    }
    
    /**
     * 从映射中移除订阅
     */
    private void removeFromMap(Map<String, Set<String>> map, String topic, String clientId) {
        Set<String> subscribers = map.get(topic);
        if (subscribers != null) {
            subscribers.remove(clientId);
            if (subscribers.isEmpty()) {
                map.remove(topic);
            }
        }
    }
    
    /**
     * 检查主题是否包含通配符
     */
    private boolean isWildcardTopic(String topic) {
        return topic.contains("+") || topic.contains("#");
    }
    
    /**
     * 统计通配符主题数量
     */
    private int countWildcardTopics() {
        return countWildcardTopicsRecursive(wildcardTrie);
    }
    
    private int countWildcardTopicsRecursive(TrieNode node) {
        int count = 0;
        for (String key : node.clients.keySet()) {
            if ("+".equals(key) || "#".equals(key)) {
                count++;
            }
        }
        for (TrieNode child : node.children.values()) {
            count += countWildcardTopicsRecursive(child);
        }
        return count;
    }
    
    /**
     * 统计通配符订阅数量
     */
    private int countWildcardSubscriptions() {
        return countWildcardSubscriptionsRecursive(wildcardTrie);
    }
    
    private int countWildcardSubscriptionsRecursive(TrieNode node) {
        int count = node.clients.values().stream().mapToInt(Set::size).sum();
        for (TrieNode child : node.children.values()) {
            count += countWildcardSubscriptionsRecursive(child);
        }
        return count;
    }
    
    /**
     * Trie节点类
     */
    private static class TrieNode {
        final Map<String, TrieNode> children = new ConcurrentHashMap<>();
        final Map<String, Set<String>> clients = new ConcurrentHashMap<>();
    }
    
    /**
     * 主题统计信息
     */
    public static class TopicStats {
        private final int totalTopics;
        private final int totalSubscriptions;
        private final int totalClients;
        
        public TopicStats(int totalTopics, int totalSubscriptions, int totalClients) {
            this.totalTopics = totalTopics;
            this.totalSubscriptions = totalSubscriptions;
            this.totalClients = totalClients;
        }
        
        public int getTotalTopics() {
            return totalTopics;
        }
        
        public int getTotalSubscriptions() {
            return totalSubscriptions;
        }
        
        public int getTotalClients() {
            return totalClients;
        }
    }
}