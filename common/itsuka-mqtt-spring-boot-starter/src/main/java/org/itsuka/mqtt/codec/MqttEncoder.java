package org.itsuka.mqtt.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.protocol.*;

import java.nio.charset.StandardCharsets;

/**
 * MQTT消息编码器
 * 
 * @author itsuka
 */
@Slf4j
public class MqttEncoder extends MessageToByteEncoder<MqttMessage> {
    
    @Override
    protected void encode(ChannelHandlerContext ctx, MqttMessage msg, ByteBuf out) throws Exception {
        try {
            // 编码固定头部
            encodeFixedHeader(msg.getFixedHeader(), out);
            
            // 编码可变头部和有效载荷
            encodeVariableHeaderAndPayload(msg, out);
            
            log.debug("Encoded MQTT message: {}", msg.getMessageType());
            
        } catch (Exception e) {
            log.error("Failed to encode MQTT message: {}", msg.getMessageType(), e);
            throw e;
        }
    }
    
    /**
     * 编码固定头部
     */
    private void encodeFixedHeader(MqttFixedHeader fixedHeader, ByteBuf out) {
        // 写入第一个字节
        out.writeByte(fixedHeader.getFirstByte());
        
        // 写入剩余长度
        encodeRemainingLength(fixedHeader.getRemainingLength(), out);
    }
    
    /**
     * 编码剩余长度
     */
    private void encodeRemainingLength(int remainingLength, ByteBuf out) {
        if (remainingLength < 0) {
            throw new IllegalArgumentException("Remaining length cannot be negative: " + remainingLength);
        }
        
        do {
            byte encodedByte = (byte) (remainingLength % 128);
            remainingLength = remainingLength / 128;
            
            if (remainingLength > 0) {
                encodedByte |= 0x80; // 设置继续位
            }
            
            out.writeByte(encodedByte);
        } while (remainingLength > 0);
    }
    
    /**
     * 编码可变头部和有效载荷
     */
    private void encodeVariableHeaderAndPayload(MqttMessage message, ByteBuf out) {
        switch (message.getMessageType()) {
            case CONNECT:
                encodeConnectMessage(message, out);
                break;
            case CONNACK:
                encodeConnAckMessage(message, out);
                break;
            case PUBLISH:
                encodePublishMessage(message, out);
                break;
            case PUBACK:
            case PUBREC:
            case PUBREL:
            case PUBCOMP:
                encodePubAckMessage(message, out);
                break;
            case SUBSCRIBE:
                encodeSubscribeMessage(message, out);
                break;
            case SUBACK:
                encodeSubAckMessage(message, out);
                break;
            case UNSUBSCRIBE:
                encodeUnsubscribeMessage(message, out);
                break;
            case UNSUBACK:
                encodeUnsubAckMessage(message, out);
                break;
            case PINGREQ:
            case PINGRESP:
                // PING消息没有可变头部和有效载荷
                break;
            case DISCONNECT:
                // DISCONNECT消息没有可变头部和有效载荷
                break;
            default:
                log.warn("Unsupported message type for encoding: {}", message.getMessageType());
                break;
        }
    }
    
    /**
     * 编码UTF-8字符串
     */
    private void encodeString(String str, ByteBuf out) {
        if (str == null) {
            out.writeShort(0);
            return;
        }
        
        byte[] bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.writeShort(bytes.length);
        out.writeBytes(bytes);
    }
    
    /**
     * 编码二进制数据
     */
    private void encodeBinaryData(byte[] data, ByteBuf out) {
        if (data == null) {
            out.writeShort(0);
            return;
        }
        
        out.writeShort(data.length);
        out.writeBytes(data);
    }
    
    // 各种消息类型的编码方法
    private void encodeConnectMessage(MqttMessage message, ByteBuf out) {
        try {
            MqttConnectVariableHeader variableHeader = (MqttConnectVariableHeader) message.getVariableHeader();
            MqttConnectPayload payload = (MqttConnectPayload) message.getPayload();
            
            if (variableHeader == null || payload == null) {
                throw new IllegalArgumentException("CONNECT message requires variableHeader and payload");
            }
            
            // 编码可变头部
            encodeString(variableHeader.getProtocolName(), out);
            out.writeByte(variableHeader.getProtocolVersion());
            out.writeByte(variableHeader.getConnectFlags().toByte());
            out.writeShort(variableHeader.getKeepAlive());
            
            // 编码有效载荷
            encodeString(payload.getClientId(), out);
            
            MqttConnectFlags flags = variableHeader.getConnectFlags();
            if (flags.isWillFlag()) {
                encodeString(payload.getWillTopic(), out);
                encodeBinaryData(payload.getWillMessage(), out);
            }
            if (flags.isUsernameFlag()) {
                encodeString(payload.getUsername(), out);
            }
            if (flags.isPasswordFlag()) {
                encodeBinaryData(payload.getPassword(), out);
            }
            
            log.debug("Encoded CONNECT message");
        } catch (Exception e) {
            log.error("Failed to encode CONNECT message", e);
            throw new IllegalArgumentException("Invalid CONNECT message", e);
        }
    }
    
    private void encodeConnAckMessage(MqttMessage message, ByteBuf out) {
        try {
            MqttConnAckVariableHeader variableHeader = (MqttConnAckVariableHeader) message.getVariableHeader();
            
            if (variableHeader == null) {
                throw new IllegalArgumentException("CONNACK message requires variableHeader");
            }
            
            // 编码连接确认标志
            byte ackFlags = (byte) (variableHeader.isSessionPresent() ? 0x01 : 0x00);
            out.writeByte(ackFlags);
            
            // 编码返回码
            out.writeByte(variableHeader.getReturnCode().getValue());
            
            log.debug("Encoded CONNACK message");
        } catch (Exception e) {
            log.error("Failed to encode CONNACK message", e);
            throw new IllegalArgumentException("Invalid CONNACK message", e);
        }
    }
    
    private void encodePublishMessage(MqttMessage message, ByteBuf out) {
        try {
            MqttPublishVariableHeader variableHeader = (MqttPublishVariableHeader) message.getVariableHeader();
            
            if (variableHeader == null) {
                throw new IllegalArgumentException("PUBLISH message requires variableHeader");
            }
            
            // 编码主题名称
            encodeString(variableHeader.getTopicName(), out);
            
            // 编码消息ID（仅当QoS > 0时）
            if (message.getQosLevel().getValue() > 0) {
                out.writeShort(variableHeader.getMessageId());
            }
            
            // 编码有效载荷
            if (message.getPayload() instanceof byte[]) {
                out.writeBytes((byte[]) message.getPayload());
            }
            
            log.debug("Encoded PUBLISH message");
        } catch (Exception e) {
            log.error("Failed to encode PUBLISH message", e);
            throw new IllegalArgumentException("Invalid PUBLISH message", e);
        }
    }
    
    private void encodePubAckMessage(MqttMessage message, ByteBuf out) {
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.getVariableHeader();
            
            if (variableHeader == null) {
                throw new IllegalArgumentException(message.getMessageType() + " message requires variableHeader");
            }
            
            // 编码消息ID
            out.writeShort(variableHeader.getMessageId());
            
            log.debug("Encoded {} message", message.getMessageType());
        } catch (Exception e) {
            log.error("Failed to encode {} message", message.getMessageType(), e);
            throw new IllegalArgumentException("Invalid " + message.getMessageType() + " message", e);
        }
    }
    
    private void encodeSubscribeMessage(MqttMessage message, ByteBuf out) {
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.getVariableHeader();
            MqttSubscribePayload payload = (MqttSubscribePayload) message.getPayload();
            
            if (variableHeader == null || payload == null) {
                throw new IllegalArgumentException("SUBSCRIBE message requires variableHeader and payload");
            }
            
            // 编码消息ID
            out.writeShort(variableHeader.getMessageId());
            
            // 编码主题订阅列表
            for (MqttTopicSubscription subscription : payload.getTopicSubscriptions()) {
                encodeString(subscription.getTopicFilter(), out);
                out.writeByte(subscription.getQos().getValue());
            }
            
            log.debug("Encoded SUBSCRIBE message");
        } catch (Exception e) {
            log.error("Failed to encode SUBSCRIBE message", e);
            throw new IllegalArgumentException("Invalid SUBSCRIBE message", e);
        }
    }
    
    private void encodeSubAckMessage(MqttMessage message, ByteBuf out) {
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.getVariableHeader();
            MqttSubAckPayload payload = (MqttSubAckPayload) message.getPayload();
            
            if (variableHeader == null || payload == null) {
                throw new IllegalArgumentException("SUBACK message requires variableHeader and payload");
            }
            
            // 编码消息ID
            out.writeShort(variableHeader.getMessageId());
            
            // 编码授予的QoS列表
            for (MqttQoS qos : payload.getGrantedQosList()) {
                out.writeByte(qos.getValue());
            }
            
            log.debug("Encoded SUBACK message");
        } catch (Exception e) {
            log.error("Failed to encode SUBACK message", e);
            throw new IllegalArgumentException("Invalid SUBACK message", e);
        }
    }
    
    private void encodeUnsubscribeMessage(MqttMessage message, ByteBuf out) {
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.getVariableHeader();
            MqttUnsubscribePayload payload = (MqttUnsubscribePayload) message.getPayload();
            
            if (variableHeader == null || payload == null) {
                throw new IllegalArgumentException("UNSUBSCRIBE message requires variableHeader and payload");
            }
            
            // 编码消息ID
            out.writeShort(variableHeader.getMessageId());
            
            // 编码主题列表
            for (String topic : payload.getTopics()) {
                encodeString(topic, out);
            }
            
            log.debug("Encoded UNSUBSCRIBE message");
        } catch (Exception e) {
            log.error("Failed to encode UNSUBSCRIBE message", e);
            throw new IllegalArgumentException("Invalid UNSUBSCRIBE message", e);
        }
    }
    
    private void encodeUnsubAckMessage(MqttMessage message, ByteBuf out) {
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.getVariableHeader();
            
            if (variableHeader == null) {
                throw new IllegalArgumentException("UNSUBACK message requires variableHeader");
            }
            
            // 编码消息ID
            out.writeShort(variableHeader.getMessageId());
            
            log.debug("Encoded UNSUBACK message");
        } catch (Exception e) {
            log.error("Failed to encode UNSUBACK message", e);
            throw new IllegalArgumentException("Invalid UNSUBACK message", e);
        }
    }
}