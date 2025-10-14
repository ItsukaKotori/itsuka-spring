package org.itsuka.mqtt.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.event.MqttEventPublisher;
import org.itsuka.mqtt.event.MqttMessageEvent;
import org.itsuka.mqtt.protocol.*;
import org.itsuka.mqtt.session.MqttSession;
import org.itsuka.mqtt.session.MqttSessionManager;

/**
 * MQTT服务器处理器
 * 
 * @author itsuka
 */
@Slf4j
public class MqttServerHandler extends SimpleChannelInboundHandler<MqttMessage> {
    
    private final MqttSessionManager sessionManager;
    private final MqttEventPublisher eventPublisher;
    
    public MqttServerHandler(MqttSessionManager sessionManager, MqttEventPublisher eventPublisher) {
        this.sessionManager = sessionManager;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client connected: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Client disconnected: {}", ctx.channel().remoteAddress());
        
        // 清理会话
        sessionManager.removeSession(ctx.channel());
        
        super.channelInactive(ctx);
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
        log.debug("Received MQTT message: {} from {}", msg.getMessageType(), ctx.channel().remoteAddress());
        
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
                ctx.close();
                return;
            }
            
            String clientId = payload.getClientId();
            boolean cleanSession = variableHeader.getConnectFlags().isCleanSession();
            
            // TODO: 验证客户端认证信息
            // 这里可以添加用户名和密码验证逻辑
            
            // 创建或恢复会话
            sessionManager.createSession(ctx.channel(), clientId, cleanSession);
            
            // 发送CONNACK响应
            sendConnAck(ctx, true, MqttConnectReturnCode.CONNECTION_ACCEPTED);
            
            log.info("Client {} connected successfully, cleanSession: {}", clientId, cleanSession);
        } catch (Exception e) {
            log.error("Error handling CONNECT message", e);
            sendConnAck(ctx, false, MqttConnectReturnCode.SERVER_UNAVAILABLE);
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
            
            log.info("Received PUBLISH message on topic: {}, QoS: {}", topic, qos);
            
            // TODO: 转发消息给订阅者
            // 这里需要从 TopicManager 获取订阅者并转发消息
            
            // 根据QoS等级发送相应的确认消息
            if (qos == MqttQoS.AT_LEAST_ONCE) {
                sendPubAck(ctx, messageId);
            } else if (qos == MqttQoS.EXACTLY_ONCE) {
                sendPubRec(ctx, messageId);
            }
        } catch (Exception e) {
            log.error("Error handling PUBLISH message", e);
        }
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
                sendPubRel(ctx, messageId);
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
                sendPubComp(ctx, messageId);
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
            
            // 处理订阅请求
            for (MqttTopicSubscription subscription : payload.getTopicSubscriptions()) {
                String topic = subscription.getTopicFilter();
                MqttQoS qos = subscription.getQos();
                
                log.info("Client subscribing to topic: {} with QoS: {}", topic, qos);
                
                // TODO: 添加订阅关系到 TopicManager
                // topicManager.addSubscription(clientId, topic, qos);
                
                // 添加授予的QoS
                ackPayload.addGrantedQoS(qos);
            }
            
            // 发送SUBACK响应
            sendSubAck(ctx, messageId, ackPayload);
        } catch (Exception e) {
            log.error("Error handling SUBSCRIBE message", e);
        }
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
            
            // 处理取消订阅请求
            for (String topic : payload.getTopics()) {
                log.info("Client unsubscribing from topic: {}", topic);
                // TODO: 从 TopicManager 移除订阅关系
                // topicManager.removeSubscription(clientId, topic);
            }
            
            // 发送UNSUBACK响应
            sendUnsubAck(ctx, messageId);
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
        sendPingResp(ctx);
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
    
    /**
     * 发送CONNACK消息
     */
    private void sendConnAck(ChannelHandlerContext ctx, boolean sessionPresent, MqttConnectReturnCode returnCode) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 2);
        MqttConnAckVariableHeader variableHeader = new MqttConnAckVariableHeader(
                sessionPresent, returnCode);
        MqttMessage connAck = new MqttMessage(fixedHeader, variableHeader);
        ctx.writeAndFlush(connAck);
    }
    
    /**
     * 发送PUBACK消息
     */
    private void sendPubAck(ChannelHandlerContext ctx, int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2);
        MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
        MqttMessage pubAck = new MqttMessage(fixedHeader, variableHeader);
        ctx.writeAndFlush(pubAck);
    }
    
    /**
     * 发送PUBREC消息
     */
    private void sendPubRec(ChannelHandlerContext ctx, int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 2);
        MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
        MqttMessage pubRec = new MqttMessage(fixedHeader, variableHeader);
        ctx.writeAndFlush(pubRec);
    }
    
    /**
     * 发送PUBREL消息
     */
    private void sendPubRel(ChannelHandlerContext ctx, int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 2);
        MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
        MqttMessage pubRel = new MqttMessage(fixedHeader, variableHeader);
        ctx.writeAndFlush(pubRel);
    }
    
    /**
     * 发送PUBCOMP消息
     */
    private void sendPubComp(ChannelHandlerContext ctx, int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 2);
        MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
        MqttMessage pubComp = new MqttMessage(fixedHeader, variableHeader);
        ctx.writeAndFlush(pubComp);
    }
    
    /**
     * 发送SUBACK消息
     */
    private void sendSubAck(ChannelHandlerContext ctx, int messageId, MqttSubAckPayload payload) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 
                2 + payload.getGrantedQosList().size());
        MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
        MqttMessage subAck = new MqttMessage(fixedHeader, variableHeader, payload);
        ctx.writeAndFlush(subAck);
    }
    
    /**
     * 发送UNSUBACK消息
     */
    private void sendUnsubAck(ChannelHandlerContext ctx, int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2);
        MqttMessageIdVariableHeader variableHeader = new MqttMessageIdVariableHeader(messageId);
        MqttMessage unsubAck = new MqttMessage(fixedHeader, variableHeader);
        ctx.writeAndFlush(unsubAck);
    }
    
    /**
     * 发送PINGRESP消息
     */
    private void sendPingResp(ChannelHandlerContext ctx) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttMessage pingResp = new MqttMessage(fixedHeader);
        ctx.writeAndFlush(pingResp);
    }
}