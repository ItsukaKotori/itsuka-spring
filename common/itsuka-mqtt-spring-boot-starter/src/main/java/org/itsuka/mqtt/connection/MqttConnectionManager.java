package org.itsuka.mqtt.connection;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.event.MqttConnectionEvent;
import org.itsuka.mqtt.event.MqttEventPublisher;
import org.itsuka.mqtt.session.MqttSession;
import org.itsuka.mqtt.session.MqttSessionManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MQTT连接管理器
 * 
 * @author itsuka
 */
@Slf4j
@Component
public class MqttConnectionManager implements DisposableBean {
    
    private final MqttSessionManager sessionManager;
    private final MqttEventPublisher eventPublisher;
    private final ScheduledExecutorService scheduler;
    
    /**
     * 连接统计信息
     */
    private final Map<String, ConnectionStats> connectionStats = new ConcurrentHashMap<>();
    
    public MqttConnectionManager(MqttSessionManager sessionManager, MqttEventPublisher eventPublisher) {
        this.sessionManager = sessionManager;
        this.eventPublisher = eventPublisher;
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // 启动定期清理任务
        startCleanupTask();
    }
    
    /**
     * 处理客户端连接
     */
    public void handleConnect(Channel channel, String clientId, boolean cleanSession) {
        try {
            log.info("Handling MQTT connection for client: {} from {}", clientId, channel.remoteAddress());
            
            // 检查是否已存在连接
            MqttSession existingSession = sessionManager.getSession(clientId);
            if (existingSession != null && existingSession.isActive()) {
                log.warn("Client {} already connected, closing existing connection", clientId);
                existingSession.getChannel().close();
            }
            
            // 创建新会话
            MqttSession session = sessionManager.createSession(channel, clientId, cleanSession);
            
            // 更新连接统计
            updateConnectionStats(clientId, true);
            
            // 发布连接事件
            eventPublisher.publishConnectionEvent(new MqttConnectionEvent(
                    channel, clientId, MqttConnectionEvent.EventType.CONNECTED));
            
            log.info("MQTT client {} connected successfully", clientId);
            
        } catch (Exception e) {
            log.error("Failed to handle MQTT connection for client: {}", clientId, e);
            
            // 发布连接失败事件
            eventPublisher.publishConnectionEvent(new MqttConnectionEvent(
                    channel, clientId, MqttConnectionEvent.EventType.AUTHENTICATION_FAILED, e.getMessage()));
        }
    }
    
    /**
     * 处理客户端断开连接
     */
    public void handleDisconnect(Channel channel) {
        MqttSession session = sessionManager.getSession(channel);
        if (session != null) {
            handleDisconnect(session.getClientId(), MqttConnectionEvent.EventType.DISCONNECTED);
        }
    }
    
    /**
     * 处理客户端断开连接
     */
    public void handleDisconnect(String clientId, MqttConnectionEvent.EventType eventType) {
        try {
            log.info("Handling MQTT disconnection for client: {}", clientId);
            
            MqttSession session = sessionManager.getSession(clientId);
            if (session != null) {
                // 更新连接统计
                updateConnectionStats(clientId, false);
                
                // 发布断开连接事件
                eventPublisher.publishConnectionEvent(new MqttConnectionEvent(
                        session.getChannel(), clientId, eventType));
                
                // 移除会话（如果是清除会话）
                if (session.isCleanSession()) {
                    sessionManager.removeSession(clientId);
                }
                
                log.info("MQTT client {} disconnected", clientId);
            }
            
        } catch (Exception e) {
            log.error("Failed to handle MQTT disconnection for client: {}", clientId, e);
        }
    }
    
    /**
     * 检查连接是否活跃
     */
    public boolean isConnected(String clientId) {
        MqttSession session = sessionManager.getSession(clientId);
        return session != null && session.isActive();
    }
    
    /**
     * 获取连接统计信息
     */
    public ConnectionStats getConnectionStats(String clientId) {
        return connectionStats.get(clientId);
    }
    
    /**
     * 获取总连接数
     */
    public int getTotalConnections() {
        return sessionManager.getActiveSessionCount();
    }
    
    /**
     * 更新连接统计信息
     */
    private void updateConnectionStats(String clientId, boolean connected) {
        connectionStats.compute(clientId, (key, stats) -> {
            if (stats == null) {
                stats = new ConnectionStats();
            }
            
            if (connected) {
                stats.incrementConnectCount();
                stats.setLastConnectTime(System.currentTimeMillis());
            } else {
                stats.incrementDisconnectCount();
                stats.setLastDisconnectTime(System.currentTimeMillis());
            }
            
            return stats;
        });
    }
    
    /**
     * 启动清理任务
     */
    private void startCleanupTask() {
        // 每5分钟清理一次非活跃会话
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sessionManager.cleanupInactiveSessions();
                log.debug("Completed inactive session cleanup");
            } catch (Exception e) {
                log.error("Error during session cleanup", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
        
        // 每小时清理一次过期的连接统计
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredStats();
                log.debug("Completed expired stats cleanup");
            } catch (Exception e) {
                log.error("Error during stats cleanup", e);
            }
        }, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * 清理过期的连接统计
     */
    private void cleanupExpiredStats() {
        long expireTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7); // 7天过期
        
        connectionStats.entrySet().removeIf(entry -> {
            ConnectionStats stats = entry.getValue();
            return stats.getLastConnectTime() < expireTime && stats.getLastDisconnectTime() < expireTime;
        });
    }
    
    /**
     * Spring Bean销毁时调用
     */
    @Override
    public void destroy() throws Exception {
        shutdown();
    }
    
    /**
     * 关闭连接管理器
     */
    public void shutdown() {
        log.info("Shutting down MQTT connection manager...");
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Scheduler did not terminate in time, forcing shutdown");
                    scheduler.shutdownNow();
                    
                    if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                        log.error("Scheduler did not terminate after forced shutdown");
                    }
                }
                log.info("MQTT connection manager shut down successfully");
            } catch (InterruptedException e) {
                log.warn("Interrupted while shutting down scheduler", e);
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 连接统计信息
     */
    public static class ConnectionStats {
        private long connectCount = 0;
        private long disconnectCount = 0;
        private long lastConnectTime = 0;
        private long lastDisconnectTime = 0;
        
        public void incrementConnectCount() {
            this.connectCount++;
        }
        
        public void incrementDisconnectCount() {
            this.disconnectCount++;
        }
        
        // Getters and setters
        public long getConnectCount() { return connectCount; }
        public long getDisconnectCount() { return disconnectCount; }
        public long getLastConnectTime() { return lastConnectTime; }
        public void setLastConnectTime(long lastConnectTime) { this.lastConnectTime = lastConnectTime; }
        public long getLastDisconnectTime() { return lastDisconnectTime; }
        public void setLastDisconnectTime(long lastDisconnectTime) { this.lastDisconnectTime = lastDisconnectTime; }
    }
}