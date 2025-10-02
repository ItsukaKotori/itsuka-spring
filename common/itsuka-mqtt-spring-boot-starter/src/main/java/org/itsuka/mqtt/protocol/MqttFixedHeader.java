package org.itsuka.mqtt.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT固定头部
 * 
 * @author itsuka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttFixedHeader {
    
    /**
     * 消息类型
     */
    private MqttMessageType messageType;
    
    /**
     * DUP标志 - 重复发送标志
     */
    private boolean dup;
    
    /**
     * QoS等级
     */
    private MqttQoS qosLevel;
    
    /**
     * RETAIN标志 - 保留消息标志
     */
    private boolean retain;
    
    /**
     * 剩余长度
     */
    private int remainingLength;
    
    /**
     * 获取第一个字节
     */
    public byte getFirstByte() {
        int firstByte = 0;
        
        // 消息类型 (bit 7-4)
        firstByte |= (messageType.getValue() << 4);
        
        // DUP标志 (bit 3)
        if (dup) {
            firstByte |= 0x08;
        }
        
        // QoS等级 (bit 2-1)
        firstByte |= (qosLevel.getValue() << 1);
        
        // RETAIN标志 (bit 0)
        if (retain) {
            firstByte |= 0x01;
        }
        
        return (byte) firstByte;
    }
    
    /**
     * 从第一个字节解析固定头部
     */
    public static MqttFixedHeader fromFirstByte(byte firstByte) {
        MqttFixedHeader header = new MqttFixedHeader();
        
        // 解析消息类型 (bit 7-4)
        int messageTypeValue = (firstByte >> 4) & 0x0F;
        header.messageType = MqttMessageType.valueOf(messageTypeValue);
        
        // 解析DUP标志 (bit 3)
        header.dup = (firstByte & 0x08) != 0;
        
        // 解析QoS等级 (bit 2-1)
        int qosValue = (firstByte >> 1) & 0x03;
        header.qosLevel = MqttQoS.valueOf(qosValue);
        
        // 解析RETAIN标志 (bit 0)
        header.retain = (firstByte & 0x01) != 0;
        
        return header;
    }
}