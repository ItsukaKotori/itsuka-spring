package org.itsuka.mqtt.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQTT CONNECT连接标志
 * 
 * @author itsuka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttConnectFlags {
    
    /**
     * 清除会话标志
     */
    private boolean cleanSession;
    
    /**
     * 遗嘱标志
     */
    private boolean willFlag;
    
    /**
     * 遗嘱QoS
     */
    private MqttQoS willQoS;
    
    /**
     * 遗嘱保留标志
     */
    private boolean willRetain;
    
    /**
     * 密码标志
     */
    private boolean passwordFlag;
    
    /**
     * 用户名标志
     */
    private boolean usernameFlag;
    
    /**
     * 从字节解析连接标志
     */
    public static MqttConnectFlags fromByte(byte flagsByte) {
        MqttConnectFlags flags = new MqttConnectFlags();
        flags.usernameFlag = (flagsByte & 0x80) != 0;
        flags.passwordFlag = (flagsByte & 0x40) != 0;
        flags.willRetain = (flagsByte & 0x20) != 0;
        flags.willQoS = MqttQoS.valueOf((flagsByte >> 3) & 0x03);
        flags.willFlag = (flagsByte & 0x04) != 0;
        flags.cleanSession = (flagsByte & 0x02) != 0;
        return flags;
    }
    
    /**
     * 转换为字节
     */
    public byte toByte() {
        int flags = 0;
        if (usernameFlag) flags |= 0x80;
        if (passwordFlag) flags |= 0x40;
        if (willRetain) flags |= 0x20;
        flags |= (willQoS.getValue() << 3);
        if (willFlag) flags |= 0x04;
        if (cleanSession) flags |= 0x02;
        return (byte) flags;
    }
}
