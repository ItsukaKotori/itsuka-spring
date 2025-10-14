package org.itsuka.mqtt.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.protocol.*;

import java.util.List;

/**
 * MQTT消息解码器
 * 
 * @author itsuka
 */
@Slf4j
public class MqttDecoder extends ByteToMessageDecoder {
    
    private static final int MAX_REMAINING_LENGTH = 268435455; // 256MB
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 2) {
            // 至少需要2个字节：固定头部第一个字节 + 剩余长度至少1个字节
            return;
        }
        
        // 标记读取位置
        in.markReaderIndex();
        
        try {
            // 读取固定头部第一个字节
            byte firstByte = in.readByte();
            MqttFixedHeader fixedHeader = MqttFixedHeader.fromFirstByte(firstByte);
            
            // 读取剩余长度
            int remainingLength = decodeRemainingLength(in);
            if (remainingLength == -1) {
                // 剩余长度不完整，重置读取位置
                in.resetReaderIndex();
                return;
            }
            
            if (remainingLength > MAX_REMAINING_LENGTH) {
                throw new IllegalArgumentException("Remaining length exceeds maximum: " + remainingLength);
            }
            
            // 检查是否有足够的字节读取完整消息
            if (in.readableBytes() < remainingLength) {
                in.resetReaderIndex();
                return;
            }
            
            fixedHeader.setRemainingLength(remainingLength);
            
            // 读取可变头部和有效载荷
            ByteBuf messageBytes = in.readSlice(remainingLength);
            
            // 根据消息类型解析消息
            MqttMessage message = decodeMessage(fixedHeader, messageBytes);
            
            if (message != null) {
                out.add(message);
                log.debug("Decoded MQTT message: {}", message.getMessageType());
            }
            
        } catch (Exception e) {
            log.error("Failed to decode MQTT message", e);
            in.resetReaderIndex();
            throw e;
        }
    }
    
    /**
     * 解码剩余长度
     */
    private int decodeRemainingLength(ByteBuf buffer) {
        int remainingLength = 0;
        int multiplier = 1;
        int byteCount = 0;
        
        while (true) {
            if (!buffer.isReadable()) {
                return -1; // 需要更多字节
            }
            
            byte encodedByte = buffer.readByte();
            remainingLength += (encodedByte & 0x7F) * multiplier;
            
            if ((encodedByte & 0x80) == 0) {
                break; // 最后一个字节
            }
            
            multiplier *= 128;
            byteCount++;
            
            if (byteCount >= 4) {
                throw new IllegalArgumentException("Remaining length field exceeds 4 bytes");
            }
        }
        
        return remainingLength;
    }
    
    /**
     * 根据消息类型解码消息
     */
    private MqttMessage decodeMessage(MqttFixedHeader fixedHeader, ByteBuf messageBytes) {
        MqttMessageType messageType = fixedHeader.getMessageType();
        
        switch (messageType) {
            case CONNECT:
                return decodeConnectMessage(fixedHeader, messageBytes);
            case CONNACK:
                return decodeConnAckMessage(fixedHeader, messageBytes);
            case PUBLISH:
                return decodePublishMessage(fixedHeader, messageBytes);
            case PUBACK:
            case PUBREC:
            case PUBREL:
            case PUBCOMP:
                return decodePubAckMessage(fixedHeader, messageBytes);
            case SUBSCRIBE:
                return decodeSubscribeMessage(fixedHeader, messageBytes);
            case SUBACK:
                return decodeSubAckMessage(fixedHeader, messageBytes);
            case UNSUBSCRIBE:
                return decodeUnsubscribeMessage(fixedHeader, messageBytes);
            case UNSUBACK:
                return decodeUnsubAckMessage(fixedHeader, messageBytes);
            case PINGREQ:
            case PINGRESP:
                return decodePingMessage(fixedHeader, messageBytes);
            case DISCONNECT:
                return decodeDisconnectMessage(fixedHeader, messageBytes);
            default:
                log.warn("Unsupported message type: {}", messageType);
                return new MqttMessage(fixedHeader);
        }
    }
    
    // 各种消息类型的解码方法
    private MqttMessage decodeConnectMessage(MqttFixedHeader fixedHeader, ByteBuf messageBytes) {
        try {
            // 读取协议名称
            String protocolName = decodeString(messageBytes);
            
            // 读取协议版本
            int protocolVersion = messageBytes.readUnsignedByte();
            
            // 读取连接标志
            MqttConnectFlags connectFlags = MqttConnectFlags.fromByte(messageBytes.readByte());
            
            // 读取保持连接时间
            int keepAlive = messageBytes.readUnsignedShort();
            
            MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(
                    protocolName, protocolVersion, connectFlags, keepAlive);
            
            // 读取有效载荷
            MqttConnectPayload payload = new MqttConnectPayload();
            
            // 读取客户端ID
            payload.setClientId(decodeString(messageBytes));
            
            // 读取遗嘱主题和消息（如果存在）
            if (connectFlags.isWillFlag()) {
                payload.setWillTopic(decodeString(messageBytes));
                payload.setWillMessage(decodeBinaryData(messageBytes));
            }
            
            // 读取用户名（如果存在）
            if (connectFlags.isUsernameFlag()) {
                payload.setUsername(decodeString(messageBytes));
            }
            
            // 读取密码（如果存在）
            if (connectFlags.isPasswordFlag()) {
                payload.setPassword(decodeBinaryData(messageBytes));
            }
            
            return new MqttMessage(fixedHeader, variableHeader, payload);
        } catch (Exception e) {
            log.error("Failed to decode CONNECT message", e);
            throw new IllegalArgumentException("Invalid CONNECT message format", e);
        }
    }
    
    private MqttMessage decodeConnAckMessage(MqttFixedHeader fixedHeader, ByteBuf messageBytes) {
        try {
            // 读取连接确认标志
            byte ackFlags = messageBytes.readByte();
            boolean sessionPresent = (ackFlags & 0x01) != 0;
            
            // 读取返回码
            int returnCodeValue = messageBytes.readUnsignedByte();
            MqttConnectReturnCode returnCode = MqttConnectReturnCode.valueOf(returnCodeValue);
            
            MqttConnAckVariableHeader variableHeader = new MqttConnAckVariableHeader(
                    sessionPresent, returnCode);
            
            return new MqttMessage(fixedHeader, variableHeader);
        } catch (Exception e) {
            log.error("Failed to decode CONNACK message", e);
            throw new IllegalArgumentException("Invalid CONNACK message format", e);
        }
    }
    
    private MqttMessage decodePublishMessage(MqttFixedHeader fixedHeader, ByteBuf messageBytes) {
        try {
            // 读取主题名称
            String topicName = decodeString(messageBytes);
            
            // 读取消息ID（仅当QoS > 0时）
            int messageId = 0;
            if (fixedHeader.getQosLevel().getValue() > 0) {
                messageId = messageBytes.readUnsignedShort();
            }
            
            MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(
                    topicName, messageId);
            
            // 读取有效载荷（剩余所有字节）
            byte[] payload = new byte[messageBytes.readableBytes()];
            messageBytes.readBytes(payload);
            
            return new MqttMessage(fixedHeader, variableHeader, payload);
        } catch (Exception e) {
            log.error("Failed to decode PUBLISH message", e);
            throw new IllegalArgumentException("Invalid PUBLISH message format", e);
        }
    }
    
    private MqttMessage decodePubAckMessage(MqttFixedHeader fixedHeader, ByteBuf messageBytes) {
        try {
            // 读取消息ID
            int messageId = messageBytes.readUnsignedShort();
            MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
            
            return new MqttMessage(fixedHeader, variableHeader);
        } catch (Exception e) {
            log.error("Failed to decode {} message", fixedHeader.getMessageType(), e);
            throw new IllegalArgumentException("Invalid message format: " + fixedHeader.getMessageType(), e);
        }
    }
    
    private MqttMessage decodeSubscribeMessage(MqttFixedHeader fixedHeader, ByteBuf messageBytes) {
        try {
            // 读取消息ID
            int messageId = messageBytes.readUnsignedShort();
            MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
            
            // 读取主题订阅列表
            MqttSubscribePayload payload = new MqttSubscribePayload();
            while (messageBytes.isReadable()) {
                String topic = decodeString(messageBytes);
                int qosValue = messageBytes.readUnsignedByte();
                MqttQoS qos = MqttQoS.valueOf(qosValue);
                payload.addSubscription(topic, qos);
            }
            
            return new MqttMessage(fixedHeader, variableHeader, payload);
        } catch (Exception e) {
            log.error("Failed to decode SUBSCRIBE message", e);
            throw new IllegalArgumentException("Invalid SUBSCRIBE message format", e);
        }
    }
    
    private MqttMessage decodeSubAckMessage(MqttFixedHeader fixedHeader, ByteBuf messageBytes) {
        try {
            // 读取消息ID
            int messageId = messageBytes.readUnsignedShort();
            MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
            
            // 读取授予的QoS列表
            MqttSubAckPayload payload = new MqttSubAckPayload();
            while (messageBytes.isReadable()) {
                int qosValue = messageBytes.readUnsignedByte();
                MqttQoS qos = MqttQoS.valueOf(qosValue);
                payload.addGrantedQoS(qos);
            }
            
            return new MqttMessage(fixedHeader, variableHeader, payload);
        } catch (Exception e) {
            log.error("Failed to decode SUBACK message", e);
            throw new IllegalArgumentException("Invalid SUBACK message format", e);
        }
    }
    
    private MqttMessage decodeUnsubscribeMessage(MqttFixedHeader fixedHeader, ByteBuf messageBytes) {
        try {
            // 读取消息ID
            int messageId = messageBytes.readUnsignedShort();
            MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
            
            // 读取主题列表
            MqttUnsubscribePayload payload = new MqttUnsubscribePayload();
            while (messageBytes.isReadable()) {
                String topic = decodeString(messageBytes);
                payload.addTopic(topic);
            }
            
            return new MqttMessage(fixedHeader, variableHeader, payload);
        } catch (Exception e) {
            log.error("Failed to decode UNSUBSCRIBE message", e);
            throw new IllegalArgumentException("Invalid UNSUBSCRIBE message format", e);
        }
    }
    
    private MqttMessage decodeUnsubAckMessage(MqttFixedHeader fixedHeader, ByteBuf messageBytes) {
        try {
            // 读取消息ID
            int messageId = messageBytes.readUnsignedShort();
            MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
            
            return new MqttMessage(fixedHeader, variableHeader);
        } catch (Exception e) {
            log.error("Failed to decode UNSUBACK message", e);
            throw new IllegalArgumentException("Invalid UNSUBACK message format", e);
        }
    }
    
    private MqttMessage decodePingMessage(MqttFixedHeader fixedHeader, ByteBuf messageBytes) {
        // PING消息没有可变头部和有效载荷
        return new MqttMessage(fixedHeader);
    }
    
    private MqttMessage decodeDisconnectMessage(MqttFixedHeader fixedHeader, ByteBuf messageBytes) {
        // DISCONNECT消息没有可变头部和有效载荷
        return new MqttMessage(fixedHeader);
    }
    
    /**
     * 解码UTF-8字符串
     */
    private String decodeString(ByteBuf buffer) {
        int length = buffer.readUnsignedShort();
        if (length == 0) {
            return "";
        }
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    /**
     * 解码二进制数据
     */
    private byte[] decodeBinaryData(ByteBuf buffer) {
        int length = buffer.readUnsignedShort();
        if (length == 0) {
            return new byte[0];
        }
        byte[] data = new byte[length];
        buffer.readBytes(data);
        return data;
    }
}