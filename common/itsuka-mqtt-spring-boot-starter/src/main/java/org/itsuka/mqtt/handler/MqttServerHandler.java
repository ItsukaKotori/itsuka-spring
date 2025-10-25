package org.itsuka.mqtt.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.auth.MqttAuthenticator;
import org.itsuka.mqtt.auth.impl.SpringSecurityMqttAuthenticator;
import org.itsuka.mqtt.event.MqttEventPublisher;
import org.itsuka.mqtt.event.MqttMessageEvent;
import org.itsuka.mqtt.protocol.*;
import org.itsuka.mqtt.security.MqttRateLimiter;
import org.itsuka.mqtt.session.MqttSession;
import org.itsuka.mqtt.session.MqttSessionManager;
import org.itsuka.mqtt.topic.MqttTopicManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.itsuka.mqtt.utils.MqttMessageFactory;

/**
 * MQTT服务器处理器
 * 
 * @author itsuka
 */
@Slf4j
@Component
public class MqttServerHandler extends SimpleChannelInboundHandler<MqttMessage> {
    
    private final MqttSessionManager sessionManager;
    private final MqttEventPublisher eventPublisher;
    private final MqttRateLimiter rateLimiter;
    private final MqttTopicManager topicManager;
    private final MqttAuthenticator authenticator;
    
    public MqttServerHandler(MqttSessionManager sessionManager, 
                           MqttEventPublisher eventPublisher,
                           MqttRateLimiter rateLimiter,
                           MqttTopicManager topicManager,
                           @Autowired(required = false) MqttAuthenticator authenticator) {
        this.sessionManager = sessionManager;
        this.eventPublisher = eventPublisher;
        this.rateLimiter = rateLimiter;
        this.topicManager = topicManager;
        this.authenticator = authenticator != null ? authenticator : new SpringSecurityMqttAuthenticator();
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client connected: {}", ctx.channel().remoteAddress());
        
        // 检查连接速率限制
        if (!rateLimiter.allowConnection(ctx)) {
            log.warn("Connection rate limit exceeded for: {}", ctx.channel().remoteAddress());
            ctx.close();
            return;
        }
        
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client disconnected: {}", ctx.channel().remoteAddress());
        
        // 清理会话
        MqttSession session = sessionManager.getSession(ctx.channel());
        if (session != null) {
            // 从所有订阅中移除客户端
            topicManager.removeAllSubscriptions(session.getClientId());
        }
        sessionManager.removeSession(ctx.channel());
        
        // 清理速率限制记录
        rateLimiter.cleanup(ctx.channel());
        
        super.channelInactive(ctx);
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
        log.debug("Received MQTT message: {} from {}", msg.getMessageType(), ctx.channel().remoteAddress());
        
        // 检查消息速率限制
        if (msg.getMessageType() != MqttMessageType.CONNECT && !rateLimiter.allowMessage(ctx)) {
            log.warn("Message rate limit exceeded for: {}", ctx.channel().remoteAddress());
            return;
        }
        
        try {
            // 处理不同类型的MQTT消息
            switch (msg.getMessageType()) {
                case CONNECT:
                    handleConnect(ctx, msg);
                    break;
                case PUBLISH:
                    handlePublish(ctx, msg);
                    break;
                case PUBACK:
                    handlePubAck(ctx, msg);
                    break;
                case PUBREC:
                    handlePubRec(ctx, msg);
                    break;
                case PUBREL:
                    handlePubRel(ctx, msg);
                    break;
                case PUBCOMP:
                    handlePubComp(ctx, msg);
                    break;
                case SUBSCRIBE:
                    handleSubscribe(ctx, msg);
                    break;
                case UNSUBSCRIBE:
                    handleUnsubscribe(ctx, msg);
                    break;
                case PINGREQ:
                    handlePingReq(ctx, msg);
                    break;
                case DISCONNECT:
                    handleDisconnect(ctx, msg);
                    break;
                default:
                    log.warn("Unsupported message type: {}", msg.getMessageType());
                    break;
            }
            
            // 发布消息事件
            eventPublisher.publishMessageEvent(new MqttMessageEvent(ctx.channel(), msg));
            
        } catch (Exception e) {
            log.error("Error processing MQTT message: {}", msg.getMessageType(), e);
            ctx.close();
        }
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            
            if (idleEvent.state() == IdleState.READER_IDLE) {
                log.warn("Client read idle timeout: {}", ctx.channel().remoteAddress());
                ctx.close();
            }
        }
        
        super.userEventTriggered(ctx, evt);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception in MQTT server handler for client: {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
    
    /**
     * 处理CONNECT消息
     */
    private void handleConnect(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Handling CONNECT message from {}", ctx.channel().remoteAddress());
        
        try {
            MqttConnectVariableHeader variableHeader = (MqttConnectVariableHeader) msg.getVariableHeader();
            MqttConnectPayload payload = (MqttConnectPayload) msg.getPayload();
            
            if (variableHeader == null || payload == null) {
                log.error("Invalid CONNECT message: missing variable header or payload");
                MqttMessageFactory.sendConnAck(ctx, false, MqttConnectReturnCode.UNACCEPTABLE_PROTOCOL_VERSION);
                ctx.close();
                return;
            }
            
            String clientId = payload.getClientId();
            
            // 验证客户端ID
            if (clientId == null || clientId.isEmpty() || clientId.length() > 65535) {
                log.error("Invalid client ID: {}", clientId);
                MqttMessageFactory.sendConnAck(ctx, false, MqttConnectReturnCode.IDENTIFIER_REJECTED);
                ctx.close();
                return;
            }
            
            boolean cleanSession = variableHeader.getConnectFlags().isCleanSession();
            
            // 认证客户端
            MqttAuthenticator.MqttAuthResult authResult = authenticator.authenticate(ctx.channel(), payload);
            if (!authResult.isSuccess()) {
                log.warn("Authentication failed for client {}: {}", clientId, authResult.getReason());
                MqttMessageFactory.sendConnAck(ctx, false, authResult.getReturnCode());
                ctx.close();
                return;
            }
            
            // 创建或恢复会话
            sessionManager.createSession(ctx.channel(), clientId, cleanSession);
            
            // 发送CONNACK响应
            MqttMessageFactory.sendConnAck(ctx, false, MqttConnectReturnCode.CONNECTION_ACCEPTED);
            
            log.info("Client {} connected successfully, cleanSession: {}", clientId, cleanSession);
        } catch (Exception e) {
            log.error("Error handling CONNECT message", e);
            MqttMessageFactory.sendConnAck(ctx, false, MqttConnectReturnCode.SERVER_UNAVAILABLE);
            ctx.close();
        }
    }
    
    /**
     * 处理PUBLISH消息
     */
    private void handlePublish(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Handling PUBLISH message from {}", ctx.channel().remoteAddress());
        
        try {
            MqttPublishVariableHeader variableHeader = (MqttPublishVariableHeader) msg.getVariableHeader();
            
            if (variableHeader == null) {
                log.error("Invalid PUBLISH message: missing variable header");
                return;
            }
            
            String topic = variableHeader.getTopicName();
            int messageId = variableHeader.getMessageId();
            MqttQoS qos = msg.getQosLevel();
            byte[] payload = msg.getPayload() != null && msg.getPayload() instanceof byte[] ? 
                (byte[]) msg.getPayload() : new byte[0];
            
            // 验证主题名称
            if (!isValidTopicName(topic)) {
                log.error("Invalid topic name: {}", topic);
                return;
            }
            
            // 验证消息大小
            if (payload.length > 268435456) { // 256MB
                log.error("Message too large: {} bytes", payload.length);
                return;
            }
            
            log.info("Received PUBLISH message on topic: {}, QoS: {}", topic, qos);
            
            // 转发消息给订阅者
            MqttSession session = sessionManager.getSession(ctx.channel());
            if (session != null) {
                topicManager.publishMessage(topic, payload, qos, session.getClientId());
            }
            
            // 根据QoS等级发送相应的确认消息
            if (qos == MqttQoS.AT_LEAST_ONCE) {
                MqttMessageFactory.sendPubAck(ctx, messageId);
            } else if (qos == MqttQoS.EXACTLY_ONCE) {
                MqttMessageFactory.sendPubRec(ctx, messageId);
            }
        } catch (Exception e) {
            log.error("Error handling PUBLISH message", e);
        }
    }
    
    /**
     * 验证主题名称
     */
    private boolean isValidTopicName(String topic) {
        if (topic == null || topic.isEmpty() || topic.length() > 65535) {
            return false;
        }
        
        // 检查是否包含非法字符
        if (topic.contains("+") || topic.contains("#")) {
            return false;
        }
        
        // 检查主题层级
        String[] levels = topic.split("/");
        if (levels.length > 128) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 处理PUBACK消息
     */
    private void handlePubAck(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Handling PUBACK message from {}", ctx.channel().remoteAddress());
        
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) msg.getVariableHeader();
            if (variableHeader != null) {
                log.info("Received PUBACK for message ID: {}", variableHeader.getMessageId());
            }
        } catch (Exception e) {
            log.error("Error handling PUBACK message", e);
        }
    }
    
    /**
     * 处理PUBREC消息
     */
    private void handlePubRec(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Handling PUBREC message from {}", ctx.channel().remoteAddress());
        
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) msg.getVariableHeader();
            if (variableHeader != null) {
                int messageId = variableHeader.getMessageId();
                log.info("Received PUBREC for message ID: {}, sending PUBREL", messageId);
                MqttMessageFactory.sendPubRel(ctx, messageId);
            }
        } catch (Exception e) {
            log.error("Error handling PUBREC message", e);
        }
    }
    
    /**
     * 处理PUBREL消息
     */
    private void handlePubRel(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Handling PUBREL message from {}", ctx.channel().remoteAddress());
        
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) msg.getVariableHeader();
            if (variableHeader != null) {
                int messageId = variableHeader.getMessageId();
                log.info("Received PUBREL for message ID: {}, sending PUBCOMP", messageId);
                MqttMessageFactory.sendPubComp(ctx, messageId);
            }
        } catch (Exception e) {
            log.error("Error handling PUBREL message", e);
        }
    }
    
    /**
     * 处理PUBCOMP消息
     */
    private void handlePubComp(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Handling PUBCOMP message from {}", ctx.channel().remoteAddress());
        
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) msg.getVariableHeader();
            if (variableHeader != null) {
                log.info("Received PUBCOMP for message ID: {}", variableHeader.getMessageId());
            }
        } catch (Exception e) {
            log.error("Error handling PUBCOMP message", e);
        }
    }
    
    /**
     * 处理SUBSCRIBE消息
     */
    private void handleSubscribe(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Handling SUBSCRIBE message from {}", ctx.channel().remoteAddress());
        
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) msg.getVariableHeader();
            MqttSubscribePayload payload = (MqttSubscribePayload) msg.getPayload();
            
            if (variableHeader == null || payload == null) {
                log.error("Invalid SUBSCRIBE message: missing variable header or payload");
                return;
            }
            
            int messageId = variableHeader.getMessageId();
            MqttSubAckPayload ackPayload = new MqttSubAckPayload();
            
            MqttSession session = sessionManager.getSession(ctx.channel());
            if (session == null) {
                log.error("No session found for SUBSCRIBE message");
                return;
            }
            
            String clientId = session.getClientId();
            
            // 处理订阅请求
            for (MqttTopicSubscription subscription : payload.getTopicSubscriptions()) {
                String topic = subscription.getTopicFilter();
                MqttQoS qos = subscription.getQos();
                
                log.info("Client {} subscribing to topic: {} with QoS: {}", clientId, topic, qos);
                
                // 验证主题过滤器
                if (!isValidTopicFilter(topic)) {
                    log.error("Invalid topic filter: {}", topic);
                    ackPayload.addGrantedQoS(MqttQoS.AT_MOST_ONCE); // Use AT_MOST_ONCE as failure indicator
                    continue;
                }
                
                // 添加订阅关系到 TopicManager
                topicManager.addSubscription(clientId, topic, qos);
                
                // 添加授予的QoS
                ackPayload.addGrantedQoS(qos);
            }
            
            // 发送SUBACK响应
            MqttMessageFactory.sendSubAck(ctx, messageId, ackPayload);
        } catch (Exception e) {
            log.error("Error handling SUBSCRIBE message", e);
        }
    }
    
    /**
     * 验证主题过滤器
     */
    private boolean isValidTopicFilter(String topicFilter) {
        if (topicFilter == null || topicFilter.isEmpty() || topicFilter.length() > 65535) {
            return false;
        }
        
        // 检查通配符使用
        String[] levels = topicFilter.split("/");
        for (String level : levels) {
            if (level.equals("#")) {
                // # 只能出现在最后一级
                if (!level.equals(levels[levels.length - 1])) {
                    return false;
                }
            } else if (level.contains("#")) {
                // # 不能与其他字符混合
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 处理UNSUBSCRIBE消息
     */
    private void handleUnsubscribe(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Handling UNSUBSCRIBE message from {}", ctx.channel().remoteAddress());
        
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) msg.getVariableHeader();
            MqttUnsubscribePayload payload = (MqttUnsubscribePayload) msg.getPayload();
            
            if (variableHeader == null || payload == null) {
                log.error("Invalid UNSUBSCRIBE message: missing variable header or payload");
                return;
            }
            
            int messageId = variableHeader.getMessageId();
            
            MqttSession session = sessionManager.getSession(ctx.channel());
            if (session == null) {
                log.error("No session found for UNSUBSCRIBE message");
                return;
            }
            
            String clientId = session.getClientId();
            
            // 处理取消订阅请求
            for (String topic : payload.getTopics()) {
                log.info("Client {} unsubscribing from topic: {}", clientId, topic);
                // 从 TopicManager 移除订阅关系
                topicManager.removeSubscription(clientId, topic);
            }
            
            // 发送UNSUBACK响应
            MqttMessageFactory.sendUnsubAck(ctx, messageId);
        } catch (Exception e) {
            log.error("Error handling UNSUBSCRIBE message", e);
        }
    }
    
    /**
     * 处理PINGREQ消息
     */
    private void handlePingReq(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Handling PINGREQ message from {}", ctx.channel().remoteAddress());
        
        // 发送PINGRESP响应
        MqttMessageFactory.sendPingResp(ctx);
    }
    
    /**
     * 处理DISCONNECT消息
     */
    private void handleDisconnect(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Handling DISCONNECT message from {}", ctx.channel().remoteAddress());
        
        // 清理会话并关闭连接
        sessionManager.removeSession(ctx.channel());
        ctx.close();
    }
    
}