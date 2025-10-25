package org.itsuka.mqtt.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.protocol.*;

/**
 * MQTT消息工厂 - 减少重复的消息创建代码
 * 
 * @author itsuka
 */
@Slf4j
public class MqttMessageFactory {
    
    /**
     * 创建CONNACK消息
     */
    public static MqttMessage createConnAck(boolean sessionPresent, MqttConnectReturnCode returnCode) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 2);
        MqttConnAckVariableHeader variableHeader = new MqttConnAckVariableHeader(sessionPresent, returnCode);
        return new MqttMessage(fixedHeader, variableHeader);
    }
    
    /**
     * 创建PUBACK消息
     */
    public static MqttMessage createPubAck(int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2);
        MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
        return new MqttMessage(fixedHeader, variableHeader);
    }
    
    /**
     * 创建PUBREC消息
     */
    public static MqttMessage createPubRec(int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 2);
        MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
        return new MqttMessage(fixedHeader, variableHeader);
    }
    
    /**
     * 创建PUBREL消息
     */
    public static MqttMessage createPubRel(int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 2);
        MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
        return new MqttMessage(fixedHeader, variableHeader);
    }
    
    /**
     * 创建PUBCOMP消息
     */
    public static MqttMessage createPubComp(int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 2);
        MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
        return new MqttMessage(fixedHeader, variableHeader);
    }
    
    /**
     * 创建SUBACK消息
     */
    public static MqttMessage createSubAck(int messageId, MqttSubAckPayload payload) {
        int remainingLength = 2 + payload.getGrantedQosList().size();
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, remainingLength);
        MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
        return new MqttMessage(fixedHeader, variableHeader, payload);
    }
    
    /**
     * 创建UNSUBACK消息
     */
    public static MqttMessage createUnsubAck(int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2);
        MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
        return new MqttMessage(fixedHeader, variableHeader);
    }
    
    /**
     * 创建PINGRESP消息
     */
    public static MqttMessage createPingResp() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        return new MqttMessage(fixedHeader);
    }
    
    /**
     * 创建PUBLISH消息
     */
    public static MqttMessage createPublish(String topic, byte[] payload, MqttQoS qos, boolean retain, int messageId) {
        int topicLength = topic.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        int payloadLength = payload != null ? payload.length : 0;
        int variableHeaderLength = 2 + topicLength + (qos != MqttQoS.AT_MOST_ONCE ? 2 : 0);
        int remainingLength = variableHeaderLength + payloadLength;
        
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBLISH, false, qos, retain, remainingLength);
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topic, messageId);
        Object messagePayload = payload != null ? payload : new byte[0];
        
        return new MqttMessage(fixedHeader, variableHeader, messagePayload);
    }
    
    /**
     * 发送消息到客户端
     */
    public static void sendMessage(ChannelHandlerContext ctx, MqttMessage message) {
        try {
            ctx.writeAndFlush(message).addListener(future -> {
                if (!future.isSuccess()) {
                    log.error("Failed to send message: {} to client: {}", 
                        message.getMessageType(), ctx.channel().remoteAddress(), future.cause());
                } else {
                    log.debug("Successfully sent message: {} to client: {}", 
                        message.getMessageType(), ctx.channel().remoteAddress());
                }
            });
        } catch (Exception e) {
            log.error("Exception while sending message: {} to client: {}", 
                message.getMessageType(), ctx.channel().remoteAddress(), e);
        }
    }
    
    /**
     * 发送CONNACK响应
     */
    public static void sendConnAck(ChannelHandlerContext ctx, boolean sessionPresent, MqttConnectReturnCode returnCode) {
        MqttMessage message = createConnAck(sessionPresent, returnCode);
        sendMessage(ctx, message);
    }
    
    /**
     * 发送PUBACK响应
     */
    public static void sendPubAck(ChannelHandlerContext ctx, int messageId) {
        MqttMessage message = createPubAck(messageId);
        sendMessage(ctx, message);
    }
    
    /**
     * 发送PUBREC响应
     */
    public static void sendPubRec(ChannelHandlerContext ctx, int messageId) {
        MqttMessage message = createPubRec(messageId);
        sendMessage(ctx, message);
    }
    
    /**
     * 发送PUBREL响应
     */
    public static void sendPubRel(ChannelHandlerContext ctx, int messageId) {
        MqttMessage message = createPubRel(messageId);
        sendMessage(ctx, message);
    }
    
    /**
     * 发送PUBCOMP响应
     */
    public static void sendPubComp(ChannelHandlerContext ctx, int messageId) {
        MqttMessage message = createPubComp(messageId);
        sendMessage(ctx, message);
    }
    
    /**
     * 发送SUBACK响应
     */
    public static void sendSubAck(ChannelHandlerContext ctx, int messageId, MqttSubAckPayload payload) {
        MqttMessage message = createSubAck(messageId, payload);
        sendMessage(ctx, message);
    }
    
    /**
     * 发送UNSUBACK响应
     */
    public static void sendUnsubAck(ChannelHandlerContext ctx, int messageId) {
        MqttMessage message = createUnsubAck(messageId);
        sendMessage(ctx, message);
    }
    
    /**
     * 发送PINGRESP响应
     */
    public static void sendPingResp(ChannelHandlerContext ctx) {
        MqttMessage message = createPingResp();
        sendMessage(ctx, message);
    }
    
    /**
     * 发布消息到客户端
     */
    public static void publishMessage(ChannelHandlerContext ctx, String topic, byte[] payload, MqttQoS qos, boolean retain, int messageId) {
        MqttMessage message = createPublish(topic, payload, qos, retain, messageId);
        sendMessage(ctx, message);
    }
}