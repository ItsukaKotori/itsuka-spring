package org.itsuka.mqtt.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * MQTT SUBACK消息有效载荷
 * 
 * @author itsuka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqttSubAckPayload {
    
    /**
     * 授予的QoS列表
     */
    private List<MqttQoS> grantedQosList;
    
    /**
     * 添加授予的QoS
     */
    public void addGrantedQoS(MqttQoS qos) {
        if (grantedQosList == null) {
            grantedQosList = new ArrayList<>();
        }
        grantedQosList.add(qos);
    }
}
