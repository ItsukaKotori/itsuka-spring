package org.itsuka.mqtt.server;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.*;

/**
 * MQTT服务器配置属性
 * 
 * @author itsuka
 */
@Data
@Validated
@ConfigurationProperties(prefix = "itsuka.mqtt.server")
public class MqttServerProperties {
    
    /**
     * 是否启用MQTT服务器
     */
    private boolean enabled = false;
    
    /**
     * 服务器端口
     */
    @Min(1)
    @Max(65535)
    private int port = 1883;
    
    /**
     * Boss线程数
     */
    @Min(1)
    @Max(16)
    private int bossThreads = 1;
    
    /**
     * Worker线程数
     */
    @Min(1)
    @Max(64)
    private int workerThreads = Math.min(Runtime.getRuntime().availableProcessors() * 2, 32);
    
    /**
     * 连接队列大小
     */
    @Min(1)
    @Max(10000)
    private int backlog = 1024;
    
    /**
     * 读空闲时间（秒）
     */
    @Min(0)
    @Max(3600)
    private int readerIdleTime = 60;
    
    /**
     * 写空闲时间（秒）
     */
    @Min(0)
    @Max(3600)
    private int writerIdleTime = 0;
    
    /**
     * 读写空闲时间（秒）
     */
    @Min(0)
    @Max(3600)
    private int allIdleTime = 0;
    
    /**
     * 最大连接数
     */
    @Min(1)
    @Max(100000)
    private int maxConnections = 10000;
    
    /**
     * 消息最大长度
     */
    @Min(1024)
    @Max(268435456)
    private int maxMessageSize = 1024 * 1024; // 1MB
    
    /**
     * 是否启用SSL
     */
    private boolean sslEnabled = false;
    
    /**
     * SSL端口
     */
    @Min(1)
    @Max(65535)
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