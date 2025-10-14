package org.itsuka.mqtt.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * MQTT消息ID生成器
 * 消息ID范围: 1-65535 (2字节无符号整数)
 * 
 * @author itsuka
 */
public class MqttMessageIdGenerator {
    
    private static final int MIN_MESSAGE_ID = 1;
    private static final int MAX_MESSAGE_ID = 65535;
    
    private final AtomicInteger messageId = new AtomicInteger(MIN_MESSAGE_ID);
    
    /**
     * 获取下一个消息ID
     */
    public int nextMessageId() {
        int id;
        do {
            id = messageId.getAndIncrement();
            if (id > MAX_MESSAGE_ID) {
                // 重置为最小值
                messageId.compareAndSet(id + 1, MIN_MESSAGE_ID);
                id = MIN_MESSAGE_ID;
            }
        } while (id < MIN_MESSAGE_ID || id > MAX_MESSAGE_ID);
        
        return id;
    }
    
    /**
     * 重置消息ID
     */
    public void reset() {
        messageId.set(MIN_MESSAGE_ID);
    }
}
