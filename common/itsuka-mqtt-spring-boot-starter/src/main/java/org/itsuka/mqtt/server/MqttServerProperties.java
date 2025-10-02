package org.itsuka.mqtt.server;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MQTT服务器配置属性
 * 
 * @author itsuka
 */
@Data
@ConfigurationProperties(prefix = "itsuka.mqtt.server")
public class MqttServerProperties {
    
    /**
     * 是否启用MQTT服务器
     */
    private boolean enabled = false;
    
    /**
     * 服务器端口
     */
    private int port = 1883;
    
    /**
     * Boss线程数
     */
    private int bossThreads = 1;
    
    /**
     * Worker线程数
     */
    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;
    
    /**
     * 连接队列大小
     */
    private int backlog = 1024;
    
    /**
     * 读空闲时间（秒）
     */
    private int readerIdleTime = 60;
    
    /**
     * 写空闲时间（秒）
     */
    private int writerIdleTime = 0;
    
    /**
     * 读写空闲时间（秒）
     */
    private int allIdleTime = 0;
    
    /**
     * 最大连接数
     */
    private int maxConnections = 10000;
    
    /**
     * 消息最大长度
     */
    private int maxMessageSize = 1024 * 1024; // 1MB
    
    /**
     * 是否启用SSL
     */
    private boolean sslEnabled = false;
    
    /**
     * SSL端口
     */
    private int sslPort = 8883;
    
    /**
     * 密钥库路径
     */
    private String keystorePath;
    
    /**
     * 密钥库密码
     */
    private String keystorePassword;
    
    /**
     * 密钥库类型
     */
    private String keystoreType = "JKS";
}