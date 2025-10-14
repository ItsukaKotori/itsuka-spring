package org.itsuka.mqtt.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.event.MqttEventPublisher;
import org.itsuka.mqtt.event.MqttMessageEvent;
import org.itsuka.mqtt.protocol.*;

/**
 * MQTT客户端处理器
 * 
 * @author itsuka
 */
@Slf4j
public class MqttClientHandler extends SimpleChannelInboundHandler<MqttMessage> {
    
    private final MqttEventPublisher eventPublisher;
    
    public MqttClientHandler(MqttEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Connected to MQTT server: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Disconnected from MQTT server: {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
        log.debug("Received MQTT message: {} from server", msg.getMessageType());
        
        try {
            // 处理不同类型的MQTT消息
            switch (msg.getMessageType()) {
                case CONNACK:
                    handleConnAck(ctx, msg);
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
                case SUBACK:
                    handleSubAck(ctx, msg);
                    break;
                case UNSUBACK:
                    handleUnsubAck(ctx, msg);
                    break;
                case PINGRESP:
                    handlePingResp(ctx, msg);
                    break;
                default:
                    log.warn("Unsupported message type: {}", msg.getMessageType());
                    break;
            }
            
            // 发布消息事件
            eventPublisher.publishMessageEvent(new MqttMessageEvent(ctx.channel(), msg));
            
        } catch (Exception e) {
            log.error("Error processing MQTT message: {}", msg.getMessageType(), e);
        }
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            
            if (idleEvent.state() == IdleState.WRITER_IDLE) {
                // 发送心跳包
                log.debug("Sending PINGREQ to server");
                // MqttMessage pingReq = createPingReqMessage();
                // ctx.writeAndFlush(pingReq);
            }
        }
        
        super.userEventTriggered(ctx, evt);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Exception in MQTT client handler", cause);
        ctx.close();
    }
    
    /**
     * 处理CONNACK消息
     */
    private void handleConnAck(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Handling CONNACK message from server");
        
        try {
            MqttConnAckVariableHeader variableHeader = (MqttConnAckVariableHeader) msg.getVariableHeader();
            
            if (variableHeader == null) {
                log.error("CONNACK message missing variable header");
                return;
            }
            
            MqttConnectReturnCode returnCode = variableHeader.getReturnCode();
            boolean sessionPresent = variableHeader.isSessionPresent();
            
            if (returnCode == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                log.info("Connection accepted by server, session present: {}", sessionPresent);
            } else {
                log.error("Connection rejected by server with code: {}", returnCode);
                ctx.close();
            }
        } catch (Exception e) {
            log.error("Error handling CONNACK message", e);
        }
    }
    
    /**
     * 处理PUBLISH消息
     */
    private void handlePublish(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Handling PUBLISH message from server");
        
        try {
            MqttPublishVariableHeader variableHeader = (MqttPublishVariableHeader) msg.getVariableHeader();
            
            if (variableHeader == null) {
                log.error("PUBLISH message missing variable header");
                return;
            }
            
            String topic = variableHeader.getTopicName();
            int messageId = variableHeader.getMessageId();
            MqttQoS qos = msg.getQosLevel();
            byte[] payload = (byte[]) msg.getPayload();
            
            log.info("Received PUBLISH message on topic: {}, QoS: {}", topic, qos);
            
            // 根据QoS等级发送相应的确认消息
            if (qos == MqttQoS.AT_LEAST_ONCE) {
                // QoS 1: 发送PUBACK
                sendPubAck(ctx, messageId);
            } else if (qos == MqttQoS.EXACTLY_ONCE) {
                // QoS 2: 发送PUBREC
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
        log.debug("Handling PUBACK message from server");
        
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) msg.getVariableHeader();
            
            if (variableHeader != null) {
                int messageId = variableHeader.getMessageId();
                log.info("Received PUBACK for message ID: {}", messageId);
                // 清理相关的消息状态（QoS 1消息已被服务器接收）
            }
        } catch (Exception e) {
            log.error("Error handling PUBACK message", e);
        }
    }
    
    /**
     * 处理PUBREC消息
     */
    private void handlePubRec(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Handling PUBREC message from server");
        
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) msg.getVariableHeader();
            
            if (variableHeader != null) {
                int messageId = variableHeader.getMessageId();
                log.info("Received PUBREC for message ID: {}, sending PUBREL", messageId);
                // QoS 2的第一步确认，发送PUBREL消息
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
        log.debug("Handling PUBREL message from server");
        
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) msg.getVariableHeader();
            
            if (variableHeader != null) {
                int messageId = variableHeader.getMessageId();
                log.info("Received PUBREL for message ID: {}, sending PUBCOMP", messageId);
                // QoS 2的第二步，发送PUBCOMP消息
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
        log.debug("Handling PUBCOMP message from server");
        
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) msg.getVariableHeader();
            
            if (variableHeader != null) {
                int messageId = variableHeader.getMessageId();
                log.info("Received PUBCOMP for message ID: {}", messageId);
                // QoS 2消息的最后确认，清理相关的消息状态
            }
        } catch (Exception e) {
            log.error("Error handling PUBCOMP message", e);
        }
    }
    
    /**
     * 处理SUBACK消息
     */
    private void handleSubAck(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Handling SUBACK message from server");
        
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) msg.getVariableHeader();
            MqttSubAckPayload payload = (MqttSubAckPayload) msg.getPayload();
            
            if (variableHeader != null && payload != null) {
                int messageId = variableHeader.getMessageId();
                log.info("Received SUBACK for message ID: {}, granted QoS: {}", 
                        messageId, payload.getGrantedQosList());
                // 更新订阅状态
            }
        } catch (Exception e) {
            log.error("Error handling SUBACK message", e);
        }
    }
    
    /**
     * 处理UNSUBACK消息
     */
    private void handleUnsubAck(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Handling UNSUBACK message from server");
        
        try {
            MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) msg.getVariableHeader();
            
            if (variableHeader != null) {
                int messageId = variableHeader.getMessageId();
                log.info("Received UNSUBACK for message ID: {}", messageId);
                // 更新订阅状态
            }
        } catch (Exception e) {
            log.error("Error handling UNSUBACK message", e);
        }
    }
    
    /**
     * 处理PINGRESP消息
     */
    private void handlePingResp(ChannelHandlerContext ctx, MqttMessage msg) {
        log.debug("Received PINGRESP from server");
        // 心跳响应，表示连接正常
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
}