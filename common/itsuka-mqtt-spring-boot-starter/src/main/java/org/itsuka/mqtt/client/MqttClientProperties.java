package org.itsuka.mqtt.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

/**
 * MQTT客户端配置属性
 * 
 * @author itsuka
 */
@Data
@ConfigurationProperties(prefix = "itsuka.mqtt.client")
public class MqttClientProperties {
    
    /**
     * 是否启用MQTT客户端
     */
    private boolean enabled = false;
    
    /**
     * 服务器主机
     */
    private String host = "localhost";
    
    /**
     * 服务器端口
     */
    private int port = 1883;
    
    /**
     * 客户端ID
     */
    private String clientId = "itsuka-mqtt-client-" + UUID.randomUUID().toString().substring(0, 8);
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * Worker线程数
     */
    private int workerThreads = Runtime.getRuntime().availableProcessors();
    
    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 30000;
    
    /**
     * 保活间隔（秒）
     */
    private int keepAliveInterval = 60;
    
    /**
     * 是否自动重连
     */
    private boolean autoReconnect = true;
    
    /**
     * 重连间隔（秒）
     */
    private int reconnectInterval = 10;
    
    /**
     * 最大重连次数
     */
    private int maxReconnectAttempts = -1; // -1表示无限重连
    
    /**
     * 是否清除会话
     */
    private boolean cleanSession = true;
    
    /**
     * 遗嘱主题
     */
    private String willTopic;
    
    /**
     * 遗嘱消息
     */
    private String willMessage;
    
    /**
     * 遗嘱QoS
     */
    private int willQos = 0;
    
    /**
     * 遗嘱保留标志
     */
    private boolean willRetain = false;
    
    /**
     * 是否启用SSL
     */
    private boolean sslEnabled = false;
    
    /**
     * 信任库路径
     */
    private String truststorePath;
    
    /**
     * 信任库密码
     */
    private String truststorePassword;
    
    /**
     * 信任库类型
     */
    private String truststoreType = "JKS";
}