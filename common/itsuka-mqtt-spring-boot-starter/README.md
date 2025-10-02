# Itsuka MQTT Spring Boot Starter

基于 Netty 的 MQTT Spring Boot Starter，提供高性能、易扩展的 MQTT 服务器和客户端功能。

## 特性

- 🚀 **高性能**: 基于 Netty 异步 I/O 框架
- 🔧 **易配置**: Spring Boot 自动配置，开箱即用
- 🎯 **易扩展**: 良好的架构设计，支持自定义扩展
- 📡 **双模式**: 同时支持 MQTT 服务器和客户端
- 🔒 **会话管理**: 完整的 MQTT 会话和连接管理
- 📊 **事件驱动**: 基于 Spring 事件机制的消息处理
- 🎪 **主题管理**: 支持通配符主题订阅和管理

## 快速开始

### 1. 添加依赖

在你的 `build.gradle` 中添加依赖：

```gradle
dependencies {
    implementation 'org.itsuka:itsuka-mqtt-spring-boot-starter:1.0.0'
}
```

### 2. 配置 MQTT 服务器

在 `application.yml` 中配置 MQTT 服务器：

```yaml
itsuka:
  mqtt:
    server:
      enabled: true
      port: 1883
      boss-threads: 1
      worker-threads: 4
      reader-idle-time: 60
      writer-idle-time: 30
      all-idle-time: 90
      max-connections: 1000
      max-message-size: 1048576
```

### 3. 配置 MQTT 客户端

```yaml
itsuka:
  mqtt:
    client:
      enabled: true
      host: localhost
      port: 1883
      client-id: mqtt-client-001
      username: your-username
      password: your-password
      keep-alive-interval: 60
      connect-timeout: 30
      auto-reconnect: true
      clean-session: true
```

### 4. 使用启用注解（更简便）

你也可以不在 `application.yml` 设置 `enabled=true`，而是在应用入口上添加注解：

```java
import org.itsuka.mqtt.annotation.EnableMqttServer; // 或 EnableMqttClient / EnableMqtt

@SpringBootApplication
@EnableMqttServer
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

- `@EnableMqttServer`: 启用服务器功能
- `@EnableMqttClient`: 启用客户端功能
- `@EnableMqtt`: 同时启用服务器与客户端

### 5. 创建消息处理器

```java
@Component
@EventListener
public class MqttMessageHandler {
    
    @EventListener
    public void handleMqttMessage(MqttMessageEvent event) {
        MqttMessage message = event.getMessage();
        String clientAddress = event.getClientAddress();
        
        // 处理 MQTT 消息
        System.out.println("Received message from " + clientAddress + ": " + message);
    }
    
    @EventListener
    public void handleConnection(MqttConnectionEvent event) {
        String clientId = event.getClientId();
        MqttConnectionEvent.EventType eventType = event.getEventType();
        
        // 处理连接事件
        System.out.println("Client " + clientId + " " + eventType);
    }
}
```

### 6. 使用 MQTT 客户端

```java
@Service
public class MqttService {
    
    @Autowired
    private MqttClient mqttClient;
    
    public void publishMessage(String topic, String payload) {
        // 创建 MQTT 消息
        MqttMessage message = new MqttMessage(/* 构造消息 */);
        
        // 发送消息
        mqttClient.sendMessage(message);
    }
    
    public void connect() {
        mqttClient.connect().thenAccept(success -> {
            if (success) {
                System.out.println("Connected to MQTT broker");
            } else {
                System.out.println("Failed to connect to MQTT broker");
            }
        });
    }
}
```

## 配置参数

### 服务器配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `itsuka.mqtt.server.enabled` | boolean | false | 是否启用 MQTT 服务器 |
| `itsuka.mqtt.server.port` | int | 1883 | 服务器端口 |
| `itsuka.mqtt.server.boss-threads` | int | 1 | Boss 线程数 |
| `itsuka.mqtt.server.worker-threads` | int | 4 | Worker 线程数 |
| `itsuka.mqtt.server.reader-idle-time` | int | 60 | 读空闲时间（秒） |
| `itsuka.mqtt.server.writer-idle-time` | int | 30 | 写空闲时间（秒） |
| `itsuka.mqtt.server.all-idle-time` | int | 90 | 全空闲时间（秒） |
| `itsuka.mqtt.server.max-connections` | int | 1000 | 最大连接数 |
| `itsuka.mqtt.server.max-message-size` | int | 1048576 | 最大消息大小（字节） |

### 客户端配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `itsuka.mqtt.client.enabled` | boolean | false | 是否启用 MQTT 客户端 |
| `itsuka.mqtt.client.host` | String | localhost | 服务器地址 |
| `itsuka.mqtt.client.port` | int | 1883 | 服务器端口 |
| `itsuka.mqtt.client.client-id` | String | - | 客户端 ID |
| `itsuka.mqtt.client.username` | String | - | 用户名 |
| `itsuka.mqtt.client.password` | String | - | 密码 |
| `itsuka.mqtt.client.keep-alive-interval` | int | 60 | 心跳间隔（秒） |
| `itsuka.mqtt.client.connect-timeout` | int | 30 | 连接超时（秒） |
| `itsuka.mqtt.client.auto-reconnect` | boolean | true | 是否自动重连 |
| `itsuka.mqtt.client.clean-session` | boolean | true | 是否清除会话 |

## 事件处理

### MQTT 消息事件

```java
@EventListener
public void handleMqttMessage(MqttMessageEvent event) {
    Channel channel = event.getChannel();
    MqttMessage message = event.getMessage();
    MqttMessageType messageType = event.getMessageType();
    String clientAddress = event.getClientAddress();
    long timestamp = event.getTimestamp();
    
    // 处理消息
}
```

### MQTT 连接事件

```java
@EventListener
public void handleConnection(MqttConnectionEvent event) {
    Channel channel = event.getChannel();
    String clientId = event.getClientId();
    MqttConnectionEvent.EventType eventType = event.getEventType();
    String clientAddress = event.getClientAddress();
    long timestamp = event.getTimestamp();
    String errorMessage = event.getErrorMessage();
    
    switch (eventType) {
        case CONNECTED:
            // 客户端连接
            break;
        case DISCONNECTED:
            // 客户端断开连接
            break;
        case AUTHENTICATION_FAILED:
            // 认证失败
            break;
        case CONNECTION_LOST:
            // 连接丢失
            break;
    }
}
```

## 高级用法

### 自定义消息处理器

```java
@Component
public class CustomMqttServerHandler extends MqttServerHandler {
    
    public CustomMqttServerHandler(MqttSessionManager sessionManager, 
                                  MqttEventPublisher eventPublisher) {
        super(sessionManager, eventPublisher);
    }
    
    @Override
    protected void handleConnect(ChannelHandlerContext ctx, MqttMessage msg) {
        // 自定义连接处理逻辑
        super.handleConnect(ctx, msg);
    }
    
    @Override
    protected void handlePublish(ChannelHandlerContext ctx, MqttMessage msg) {
        // 自定义发布消息处理逻辑
        super.handlePublish(ctx, msg);
    }
}
```

### 会话管理

```java
@Service
public class SessionService {
    
    @Autowired
    private MqttSessionManager sessionManager;
    
    public void getSessionInfo(String clientId) {
        MqttSession session = sessionManager.getSession(clientId);
        if (session != null) {
            System.out.println("Client: " + session.getClientId());
            System.out.println("Active: " + session.isActive());
            System.out.println("Subscriptions: " + session.getSubscriptions());
        }
    }
    
    public int getActiveConnections() {
        return sessionManager.getActiveSessionCount();
    }
}
```

### 主题管理

```java
@Service
public class TopicService {
    
    @Autowired
    private MqttTopicManager topicManager;
    
    public void getTopicSubscribers(String topic) {
        Set<String> subscribers = topicManager.getSubscribers(topic);
        System.out.println("Topic " + topic + " has " + subscribers.size() + " subscribers");
    }
    
    public void getClientSubscriptions(String clientId) {
        Set<String> topics = topicManager.getClientSubscriptions(clientId);
        System.out.println("Client " + clientId + " subscribed to: " + topics);
    }
}
```

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
├─────────────────────────────────────────────────────────────┤
│  MqttAutoConfiguration  │  Event Listeners  │  Services     │
├─────────────────────────────────────────────────────────────┤
│                    Management Layer                         │
├─────────────────────────────────────────────────────────────┤
│ MqttConnectionManager │ MqttSessionManager │ MqttTopicManager│
├─────────────────────────────────────────────────────────────┤
│                     Handler Layer                           │
├─────────────────────────────────────────────────────────────┤
│   MqttServerHandler   │   MqttClientHandler │ MqttEventPublisher│
├─────────────────────────────────────────────────────────────┤
│                     Protocol Layer                          │
├─────────────────────────────────────────────────────────────┤
│     MqttDecoder       │     MqttEncoder     │  MqttMessage  │
├─────────────────────────────────────────────────────────────┤
│                    Transport Layer                          │
├─────────────────────────────────────────────────────────────┤
│              Netty (NIO, Channel Pipeline)                  │
└─────────────────────────────────────────────────────────────┘
```

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！