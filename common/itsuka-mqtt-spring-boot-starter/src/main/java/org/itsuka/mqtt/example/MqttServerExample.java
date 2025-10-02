package org.itsuka.mqtt.example;

import org.itsuka.mqtt.event.MqttConnectionEvent;
import org.itsuka.mqtt.event.MqttMessageEvent;
import org.itsuka.mqtt.session.MqttSessionManager;
import org.itsuka.mqtt.topic.MqttTopicManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * MQTT 服务器示例
 * 
 * 演示如何使用 Itsuka MQTT Spring Boot Starter 创建一个 MQTT 服务器
 */
@SpringBootApplication
public class MqttServerExample {

    public static void main(String[] args) {
        SpringApplication.run(MqttServerExample.class, args);
    }

    /**
     * MQTT 消息和连接事件处理器
     */
    @Component
    public static class MqttEventHandler {

        @Autowired
        private MqttSessionManager sessionManager;

        @Autowired
        private MqttTopicManager topicManager;

        /**
         * 处理 MQTT 消息事件
         */
        @EventListener
        public void handleMqttMessage(MqttMessageEvent event) {
            System.out.println("=== MQTT Message Event ===");
            System.out.println("Client Address: " + event.getClientAddress());
            System.out.println("Message Type: " + event.getMessageType());
            System.out.println("Timestamp: " + event.getTimestamp());
            System.out.println("Channel: " + event.getChannel().id());
            
            // 根据消息类型进行不同处理
            switch (event.getMessageType()) {
                case PUBLISH:
                    handlePublishMessage(event);
                    break;
                case SUBSCRIBE:
                    handleSubscribeMessage(event);
                    break;
                case UNSUBSCRIBE:
                    handleUnsubscribeMessage(event);
                    break;
                default:
                    System.out.println("Other message type: " + event.getMessageType());
                    break;
            }
            System.out.println();
        }

        /**
         * 处理 MQTT 连接事件
         */
        @EventListener
        public void handleConnectionEvent(MqttConnectionEvent event) {
            System.out.println("=== MQTT Connection Event ===");
            System.out.println("Client ID: " + event.getClientId());
            System.out.println("Event Type: " + event.getEventType());
            System.out.println("Client Address: " + event.getClientAddress());
            System.out.println("Timestamp: " + event.getTimestamp());
            
            if (event.getErrorMessage() != null) {
                System.out.println("Error Message: " + event.getErrorMessage());
            }

            // 根据连接事件类型进行不同处理
            switch (event.getEventType()) {
                case CONNECTED:
                    handleClientConnected(event);
                    break;
                case DISCONNECTED:
                    handleClientDisconnected(event);
                    break;
                case AUTHENTICATION_FAILED:
                    handleAuthenticationFailed(event);
                    break;
                case CONNECTION_LOST:
                    handleConnectionLost(event);
                    break;
            }
            
            // 打印当前活跃连接数
            System.out.println("Active Sessions: " + sessionManager.getActiveSessionCount());
            System.out.println();
        }

        /**
         * 处理发布消息
         */
        private void handlePublishMessage(MqttMessageEvent event) {
            System.out.println("Processing PUBLISH message...");
            // 这里可以添加自定义的消息处理逻辑
            // 例如：消息持久化、消息转发、业务逻辑处理等
        }

        /**
         * 处理订阅消息
         */
        private void handleSubscribeMessage(MqttMessageEvent event) {
            System.out.println("Processing SUBSCRIBE message...");
            // 这里可以添加自定义的订阅处理逻辑
            // 例如：权限检查、订阅统计等
        }

        /**
         * 处理取消订阅消息
         */
        private void handleUnsubscribeMessage(MqttMessageEvent event) {
            System.out.println("Processing UNSUBSCRIBE message...");
            // 这里可以添加自定义的取消订阅处理逻辑
        }

        /**
         * 处理客户端连接
         */
        private void handleClientConnected(MqttConnectionEvent event) {
            System.out.println("Client connected: " + event.getClientId());
            
            // 可以在这里添加连接后的处理逻辑
            // 例如：发送欢迎消息、记录连接日志等
        }

        /**
         * 处理客户端断开连接
         */
        private void handleClientDisconnected(MqttConnectionEvent event) {
            System.out.println("Client disconnected: " + event.getClientId());
            
            // 可以在这里添加断开连接后的处理逻辑
            // 例如：清理资源、记录断开日志等
        }

        /**
         * 处理认证失败
         */
        private void handleAuthenticationFailed(MqttConnectionEvent event) {
            System.out.println("Authentication failed for client: " + event.getClientId());
            
            // 可以在这里添加认证失败的处理逻辑
            // 例如：记录安全日志、IP 黑名单等
        }

        /**
         * 处理连接丢失
         */
        private void handleConnectionLost(MqttConnectionEvent event) {
            System.out.println("Connection lost for client: " + event.getClientId());
            
            // 可以在这里添加连接丢失的处理逻辑
            // 例如：重连机制、状态恢复等
        }
    }
}