package org.itsuka.mqtt.utils;

import org.itsuka.mqtt.protocol.*;

import java.nio.charset.StandardCharsets;

/**
 * MQTT消息构建器
 * 
 * @author itsuka
 */
public class MqttMessageBuilder {
    
    /**
     * 创建CONNECT消息
     */
    public static MqttMessage createConnectMessage(String clientId, String username, String password,
                                                    int keepAlive, boolean cleanSession) {
        // 创建固定头部
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
        
        // 创建连接标志
        MqttConnectFlags flags = new MqttConnectFlags();
        flags.setCleanSession(cleanSession);
        flags.setUsernameFlag(username != null);
        flags.setPasswordFlag(password != null);
        flags.setWillFlag(false);
        
        // 创建可变头部
        MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(
                "MQTT", 4, flags, keepAlive);
        
        // 创建有效载荷
        MqttConnectPayload payload = new MqttConnectPayload();
        payload.setClientId(clientId);
        if (username != null) {
            payload.setUsername(username);
        }
        if (password != null) {
            payload.setPassword(password.getBytes(StandardCharsets.UTF_8));
        }
        
        return new MqttMessage(fixedHeader, variableHeader, payload);
    }
    
    /**
     * 创建PUBLISH消息
     */
    public static MqttMessage createPublishMessage(String topic, byte[] payload, MqttQoS qos,
                                                    boolean retain, int messageId) {
        // 创建固定头部
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBLISH, false, qos, retain, 0);
        
        // 创建可变头部
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topic, messageId);
        
        return new MqttMessage(fixedHeader, variableHeader, payload);
    }
    
    /**
     * 创建SUBSCRIBE消息
     */
    public static MqttMessage createSubscribeMessage(int messageId, String... topics) {
        // 创建固定头部
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        
        // 创建可变头部
        MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
        
        // 创建有效载荷
        MqttSubscribePayload payload = new MqttSubscribePayload();
        for (String topic : topics) {
            payload.addSubscription(topic, MqttQoS.AT_LEAST_ONCE);
        }
        
        return new MqttMessage(fixedHeader, variableHeader, payload);
    }
    
    /**
     * 创建UNSUBSCRIBE消息
     */
    public static MqttMessage createUnsubscribeMessage(int messageId, String... topics) {
        // 创建固定头部
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        
        // 创建可变头部
        MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
        
        // 创建有效载荷
        MqttUnsubscribePayload payload = new MqttUnsubscribePayload();
        for (String topic : topics) {
            payload.addTopic(topic);
        }
        
        return new MqttMessage(fixedHeader, variableHeader, payload);
    }
    
    /**
     * 创建PINGREQ消息
     */
    public static MqttMessage createPingReqMessage() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0);
        return new MqttMessage(fixedHeader);
    }
    
    /**
     * 创建DISCONNECT消息
     */
    public static MqttMessage createDisconnectMessage() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
        return new MqttMessage(fixedHeader);
    }
}
