package org.itsuka.mqtt.security;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MQTT速率限制器
 * 
 * @author itsuka
 */
@Slf4j
@Component
public class MqttRateLimiter {
    
    private final ConcurrentMap<String, ClientRateInfo> clientRateMap = new ConcurrentHashMap<>();
    
    private final int maxMessagesPerSecond;
    private final int maxConnectionsPerMinute;
    
    public MqttRateLimiter() {
        this(100, 10); // 默认值：每秒100条消息，每分钟10个连接
    }
    
    public MqttRateLimiter(int maxMessagesPerSecond, int maxConnectionsPerMinute) {
        this.maxMessagesPerSecond = maxMessagesPerSecond;
        this.maxConnectionsPerMinute = maxConnectionsPerMinute;
    }
    
    /**
     * 检查是否允许连接
     */
    public boolean allowConnection(ChannelHandlerContext ctx) {
        String clientIp = getClientIp(ctx.channel());
        ClientRateInfo rateInfo = clientRateMap.computeIfAbsent(clientIp, k -> new ClientRateInfo());
        
        long currentTime = System.currentTimeMillis();
        long minuteAgo = currentTime - 60000; // 1分钟前
        
        // 清理过期的连接记录
        rateInfo.connectionTimestamps.removeIf(timestamp -> timestamp < minuteAgo);
        
        // 检查连接频率
        if (rateInfo.connectionTimestamps.size() >= maxConnectionsPerMinute) {
            log.warn("Connection rate limit exceeded for IP: {}", clientIp);
            return false;
        }
        
        rateInfo.connectionTimestamps.add(currentTime);
        return true;
    }
    
    /**
     * 检查是否允许发送消息
     */
    public boolean allowMessage(ChannelHandlerContext ctx) {
        String clientIp = getClientIp(ctx.channel());
        ClientRateInfo rateInfo = clientRateMap.computeIfAbsent(clientIp, k -> new ClientRateInfo());
        
        long currentTime = System.currentTimeMillis();
        long secondAgo = currentTime - 1000; // 1秒前
        
        // 清理过期的消息记录
        rateInfo.messageTimestamps.removeIf(timestamp -> timestamp < secondAgo);
        
        // 检查消息频率
        if (rateInfo.messageTimestamps.size() >= maxMessagesPerSecond) {
            log.warn("Message rate limit exceeded for IP: {}", clientIp);
            return false;
        }
        
        rateInfo.messageTimestamps.add(currentTime);
        return true;
    }
    
    /**
     * 清理客户端记录
     */
    public void cleanup(Channel channel) {
        String clientIp = getClientIp(channel);
        clientRateMap.remove(clientIp);
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp(Channel channel) {
        String remoteAddress = channel.remoteAddress().toString();
        // 提取IP地址，去除端口号
        if (remoteAddress.contains("/")) {
            remoteAddress = remoteAddress.substring(1);
        }
        if (remoteAddress.contains(":")) {
            remoteAddress = remoteAddress.substring(0, remoteAddress.indexOf(":"));
        }
        return remoteAddress;
    }
    
    /**
     * 客户端速率信息
     */
    private static class ClientRateInfo {
        private final java.util.List<Long> messageTimestamps = new java.util.concurrent.CopyOnWriteArrayList<>();
        private final java.util.List<Long> connectionTimestamps = new java.util.concurrent.CopyOnWriteArrayList<>();
    }
}