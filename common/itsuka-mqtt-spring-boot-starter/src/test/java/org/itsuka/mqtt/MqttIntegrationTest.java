package org.itsuka.mqtt;

import io.netty.handler.codec.mqtt.*;
import org.itsuka.mqtt.client.MqttClient;
import org.itsuka.mqtt.event.MqttConnectionEvent;
import org.itsuka.mqtt.event.MqttMessageEvent;
import org.itsuka.mqtt.server.MqttServer;
import org.itsuka.mqtt.session.MqttSessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MQTT 集成测试
 * 
 * 测试 MQTT 服务器和客户端的完整功能
 */
@SpringBootTest(classes = TestApplication.class)
@TestPropertySource(properties = {
    "itsuka.mqtt.server.enabled=true",
    "itsuka.mqtt.server.port=1884",
    "itsuka.mqtt.client.enabled=true",
    "itsuka.mqtt.client.host=localhost",
    "itsuka.mqtt.client.port=1884",
    "itsuka.mqtt.client.client-id=test-client",
    "spring.main.allow-bean-definition-overriding=true"
})
public class MqttIntegrationTest {

    @Autowired
    private MqttServer mqttServer;

    @Autowired
    private MqttClient mqttClient;

    @Autowired
    private MqttSessionManager sessionManager;

    private CountDownLatch connectionLatch;
    private CountDownLatch messageLatch;
    private volatile MqttConnectionEvent lastConnectionEvent;
    private volatile MqttMessageEvent lastMessageEvent;

    @BeforeEach
    void setUp() throws Exception {
        connectionLatch = new CountDownLatch(1);
        messageLatch = new CountDownLatch(1);
        
        // 启动服务器
        mqttServer.start().get(5, TimeUnit.SECONDS);
        
        // 等待服务器启动
        Thread.sleep(1000);
    }

    @AfterEach
    void tearDown() throws Exception {
        // 断开客户端连接
        if (mqttClient != null) {
            mqttClient.disconnect();
        }
        
        // 停止服务器
        if (mqttServer != null) {
            mqttServer.stop().get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void testClientServerConnection() throws Exception {
        // 连接客户端
        CompletableFuture<Boolean> connectFuture = mqttClient.connect();
        Boolean connected = connectFuture.get(10, TimeUnit.SECONDS);
        
        assertTrue(connected, "Client should connect successfully");
        
        // 等待连接事件
        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS), "Should receive connection event");
        
        assertNotNull(lastConnectionEvent);
        assertEquals(MqttConnectionEvent.EventType.CONNECTED, lastConnectionEvent.getEventType());
        assertEquals("test-client", lastConnectionEvent.getClientId());
        
        // 验证会话管理器中有活跃会话
        assertTrue(sessionManager.hasSession("test-client"));
        assertEquals(1, sessionManager.getActiveSessionCount());
    }

    @Test
    void testMessagePublishing() throws Exception {
        // 先连接客户端
        testClientServerConnection();
        
        // 重置消息计数器
        messageLatch = new CountDownLatch(1);
        
        // 创建并发送发布消息
        String topic = "test/topic";
        String payload = "Hello, MQTT!";
        MqttPublishMessage publishMessage = createPublishMessage(topic, payload);
        
        mqttClient.sendMessage(publishMessage);
        
        // 等待消息事件
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Should receive message event");
        
        assertNotNull(lastMessageEvent);
        assertEquals(MqttMessageType.PUBLISH, lastMessageEvent.getMessageType());
    }

    @Test
    void testSubscription() throws Exception {
        // 先连接客户端
        testClientServerConnection();
        
        // 重置消息计数器
        messageLatch = new CountDownLatch(1);
        
        // 创建并发送订阅消息
        MqttSubscribeMessage subscribeMessage = createSubscribeMessage();
        mqttClient.sendMessage(subscribeMessage);
        
        // 等待订阅消息事件
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Should receive subscribe message event");
        
        assertNotNull(lastMessageEvent);
        assertEquals(MqttMessageType.SUBSCRIBE, lastMessageEvent.getMessageType());
    }

    @Test
    void testClientDisconnection() throws Exception {
        // 先连接客户端
        testClientServerConnection();
        
        // 重置连接计数器
        connectionLatch = new CountDownLatch(1);
        
        // 断开客户端连接
        mqttClient.disconnect();
        
        // 等待断开连接事件
        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS), "Should receive disconnection event");
        
        assertNotNull(lastConnectionEvent);
        assertEquals(MqttConnectionEvent.EventType.DISCONNECTED, lastConnectionEvent.getEventType());
        
        // 验证会话管理器中没有活跃会话
        assertEquals(0, sessionManager.getActiveSessionCount());
    }

    /**
     * 创建发布消息
     */
    private MqttPublishMessage createPublishMessage(String topic, String payload) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
            MqttMessageType.PUBLISH,
            false, // isDup
            MqttQoS.AT_MOST_ONCE,
            false, // isRetain
            0 // remainingLength (will be calculated)
        );

        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topic, 0);
        
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        io.netty.buffer.ByteBuf payloadBuffer = io.netty.buffer.Unpooled.wrappedBuffer(payloadBytes);

        return new MqttPublishMessage(fixedHeader, variableHeader, payloadBuffer);
    }

    /**
     * 创建订阅消息
     */
    private MqttSubscribeMessage createSubscribeMessage() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
            MqttMessageType.SUBSCRIBE,
            false, // isDup
            MqttQoS.AT_LEAST_ONCE,
            false, // isRetain
            0 // remainingLength (will be calculated)
        );

        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(1);

        MqttSubscribePayload payload = new MqttSubscribePayload(
            java.util.Arrays.asList(
                new MqttTopicSubscription("test/topic", MqttQoS.AT_MOST_ONCE)
            )
        );

        return new MqttSubscribeMessage(fixedHeader, variableHeader, payload);
    }

    /**
     * 处理连接事件
     */
    @EventListener
    public void handleConnectionEvent(MqttConnectionEvent event) {
        this.lastConnectionEvent = event;
        connectionLatch.countDown();
    }

    /**
     * 处理消息事件
     */
    @EventListener
    public void handleMessageEvent(MqttMessageEvent event) {
        this.lastMessageEvent = event;
        messageLatch.countDown();
    }
}