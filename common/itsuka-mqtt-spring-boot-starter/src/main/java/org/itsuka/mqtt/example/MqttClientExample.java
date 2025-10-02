package org.itsuka.mqtt.example;

import io.netty.handler.codec.mqtt.*;
import org.itsuka.mqtt.client.MqttClient;
import org.itsuka.mqtt.event.MqttConnectionEvent;
import org.itsuka.mqtt.event.MqttMessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MQTT 客户端示例
 * 
 * 演示如何使用 Itsuka MQTT Spring Boot Starter 创建一个 MQTT 客户端
 */
@SpringBootApplication
public class MqttClientExample {

    public static void main(String[] args) {
        SpringApplication.run(MqttClientExample.class, args);
    }

    /**
     * MQTT 客户端服务
     */
    @Service
    public static class MqttClientService implements CommandLineRunner {

        @Autowired
        private MqttClient mqttClient;

        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        @Override
        public void run(String... args) throws Exception {
            // 启动客户端示例
            startClientExample();
        }

        /**
         * 启动客户端示例
         */
        private void startClientExample() {
            System.out.println("Starting MQTT Client Example...");

            // 连接到 MQTT 服务器
            connectToServer();

            // 定期发送消息
            scheduleMessagePublishing();

            // 订阅主题
            subscribeToTopics();
        }

        /**
         * 连接到 MQTT 服务器
         */
        private void connectToServer() {
            CompletableFuture<Boolean> connectFuture = mqttClient.connect();
            
            connectFuture.thenAccept(success -> {
                if (success) {
                    System.out.println("Successfully connected to MQTT broker!");
                } else {
                    System.out.println("Failed to connect to MQTT broker!");
                }
            }).exceptionally(throwable -> {
                System.err.println("Connection error: " + throwable.getMessage());
                return null;
            });
        }

        /**
         * 订阅主题
         */
        private void subscribeToTopics() {
            // 延迟 2 秒后订阅主题，确保连接已建立
            scheduler.schedule(() -> {
                try {
                    // 创建订阅消息
                    MqttSubscribeMessage subscribeMessage = createSubscribeMessage();
                    mqttClient.sendMessage(subscribeMessage);
                    System.out.println("Subscribed to topics: test/topic1, test/topic2");
                } catch (Exception e) {
                    System.err.println("Failed to subscribe: " + e.getMessage());
                }
            }, 2, TimeUnit.SECONDS);
        }

        /**
         * 定期发送消息
         */
        private void scheduleMessagePublishing() {
            // 延迟 3 秒后开始，每 10 秒发送一次消息
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    publishTestMessage();
                } catch (Exception e) {
                    System.err.println("Failed to publish message: " + e.getMessage());
                }
            }, 3, 10, TimeUnit.SECONDS);
        }

        /**
         * 发布测试消息
         */
        private void publishTestMessage() {
            String topic = "test/topic1";
            String payload = "Hello from MQTT Client at " + System.currentTimeMillis();
            
            MqttPublishMessage publishMessage = createPublishMessage(topic, payload);
            mqttClient.sendMessage(publishMessage);
            
            System.out.println("Published message to " + topic + ": " + payload);
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
                    new MqttTopicSubscription("test/topic1", MqttQoS.AT_MOST_ONCE),
                    new MqttTopicSubscription("test/topic2", MqttQoS.AT_MOST_ONCE)
                )
            );

            return new MqttSubscribeMessage(fixedHeader, variableHeader, payload);
        }

        /**
         * 断开连接
         */
        public void disconnect() {
            mqttClient.disconnect();
            scheduler.shutdown();
        }
    }

    /**
     * MQTT 事件处理器
     */
    @Component
    public static class MqttClientEventHandler {

        /**
         * 处理 MQTT 消息事件
         */
        @EventListener
        public void handleMqttMessage(MqttMessageEvent event) {
            System.out.println("=== Client Received MQTT Message ===");
            System.out.println("Message Type: " + event.getMessageType());
            System.out.println("From: " + event.getClientAddress());
            System.out.println("Timestamp: " + event.getTimestamp());

            // 根据消息类型进行处理
            switch (event.getMessageType()) {
                case CONNACK:
                    handleConnAck(event);
                    break;
                case PUBLISH:
                    handlePublish(event);
                    break;
                case PUBACK:
                    handlePubAck(event);
                    break;
                case SUBACK:
                    handleSubAck(event);
                    break;
                case UNSUBACK:
                    handleUnsubAck(event);
                    break;
                case PINGRESP:
                    handlePingResp(event);
                    break;
                default:
                    System.out.println("Other message type: " + event.getMessageType());
                    break;
            }
            System.out.println();
        }

        /**
         * 处理连接事件
         */
        @EventListener
        public void handleConnectionEvent(MqttConnectionEvent event) {
            System.out.println("=== Client Connection Event ===");
            System.out.println("Event Type: " + event.getEventType());
            System.out.println("Client ID: " + event.getClientId());
            System.out.println("Timestamp: " + event.getTimestamp());
            
            if (event.getErrorMessage() != null) {
                System.out.println("Error: " + event.getErrorMessage());
            }
            System.out.println();
        }

        /**
         * 处理连接确认
         */
        private void handleConnAck(MqttMessageEvent event) {
            System.out.println("Received CONNACK - Connection acknowledged");
        }

        /**
         * 处理发布消息
         */
        private void handlePublish(MqttMessageEvent event) {
            System.out.println("Received PUBLISH message");
            // 这里可以解析消息内容
        }

        /**
         * 处理发布确认
         */
        private void handlePubAck(MqttMessageEvent event) {
            System.out.println("Received PUBACK - Publish acknowledged");
        }

        /**
         * 处理订阅确认
         */
        private void handleSubAck(MqttMessageEvent event) {
            System.out.println("Received SUBACK - Subscription acknowledged");
        }

        /**
         * 处理取消订阅确认
         */
        private void handleUnsubAck(MqttMessageEvent event) {
            System.out.println("Received UNSUBACK - Unsubscription acknowledged");
        }

        /**
         * 处理心跳响应
         */
        private void handlePingResp(MqttMessageEvent event) {
            System.out.println("Received PINGRESP - Ping response");
        }
    }
}