package org.itsuka.mqtt.auth;

import io.netty.channel.Channel;
import org.itsuka.mqtt.protocol.MqttConnectPayload;
import org.itsuka.mqtt.protocol.MqttConnectReturnCode;

/**
 * MQTT认证接口
 * 
 * @author itsuka
 */
public interface MqttAuthenticator {
    
    /**
     * 认证客户端连接
     * 
     * @param channel Netty通道
     * @param payload CONNECT消息载荷
     * @return 认证结果
     */
    MqttAuthResult authenticate(Channel channel, MqttConnectPayload payload);
    
    /**
     * 认证结果
     */
    class MqttAuthResult {
        private final boolean success;
        private final MqttConnectReturnCode returnCode;
        private final String reason;
        
        private MqttAuthResult(boolean success, MqttConnectReturnCode returnCode, String reason) {
            this.success = success;
            this.returnCode = returnCode;
            this.reason = reason;
        }
        
        public static MqttAuthResult success() {
            return new MqttAuthResult(true, MqttConnectReturnCode.CONNECTION_ACCEPTED, null);
        }
        
        public static MqttAuthResult failure(MqttConnectReturnCode returnCode, String reason) {
            return new MqttAuthResult(false, returnCode, reason);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public MqttConnectReturnCode getReturnCode() {
            return returnCode;
        }
        
        public String getReason() {
            return reason;
        }
    }
}