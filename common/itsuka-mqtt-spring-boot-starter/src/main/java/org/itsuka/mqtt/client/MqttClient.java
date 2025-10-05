package org.itsuka.mqtt.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.codec.MqttDecoder;
import org.itsuka.mqtt.codec.MqttEncoder;
import org.itsuka.mqtt.event.MqttEventPublisher;
import org.itsuka.mqtt.handler.MqttClientHandler;
import org.itsuka.mqtt.protocol.MqttMessage;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * MQTT客户端
 * 
 * @author itsuka
 */
@Slf4j
public class MqttClient implements DisposableBean {
    
    private final MqttClientProperties properties;
    private final MqttEventPublisher eventPublisher;
    
    private EventLoopGroup workerGroup;
    private Channel clientChannel;
    private volatile boolean connected = false;
    
    public MqttClient(MqttClientProperties properties, MqttEventPublisher eventPublisher) {
        this.properties = properties;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    public void destroy() throws Exception {
        disconnect();
    }
    
    /**
     * 连接到MQTT服务器
     */
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                doConnect();
                return true;
            } catch (Exception e) {
                log.error("Failed to connect to MQTT server", e);
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * 执行连接操作
     */
    private void doConnect() throws InterruptedException {
        if (connected) {
            log.warn("MQTT client is already connected");
            return;
        }
        
        log.info("Connecting to MQTT server {}:{}", properties.getHost(), properties.getPort());
        
        workerGroup = new NioEventLoopGroup(properties.getWorkerThreads());
        
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeout())
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // MQTT解码器
                            pipeline.addLast("decoder", new MqttDecoder());
                            
                            // MQTT编码器
                            pipeline.addLast("encoder", new MqttEncoder());
                            
                            // 空闲状态处理器
                            pipeline.addLast("idleStateHandler", new IdleStateHandler(
                                    properties.getKeepAliveInterval() + 10, 
                                    properties.getKeepAliveInterval(), 
                                    0, 
                                    TimeUnit.SECONDS));
                            
                            // MQTT客户端处理器
                            pipeline.addLast("mqttClientHandler", new MqttClientHandler(eventPublisher));
                        }
                    });
            
            // 连接服务器
            ChannelFuture future = bootstrap.connect(properties.getHost(), properties.getPort()).sync();
            clientChannel = future.channel();
            connected = true;
            
            log.info("Connected to MQTT server {}:{}", properties.getHost(), properties.getPort());
            
            // 监听连接关闭
            future.channel().closeFuture().addListener((ChannelFutureListener) channelFuture -> {
                connected = false;
                log.info("Disconnected from MQTT server");
                
                // 自动重连
                if (properties.isAutoReconnect()) {
                    scheduleReconnect();
                }
            });
            
        } catch (Exception e) {
            connected = false;
            log.error("Failed to connect to MQTT server", e);
            disconnect();
            throw e;
        }
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        log.info("Disconnecting from MQTT server...");
        
        connected = false;
        
        try {
            if (clientChannel != null) {
                clientChannel.close().sync();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while closing client channel", e);
            Thread.currentThread().interrupt();
        } finally {
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
        }
        
        log.info("Disconnected from MQTT server successfully");
    }
    
    /**
     * 发送消息
     */
    public CompletableFuture<Void> sendMessage(MqttMessage message) {
        if (!connected || clientChannel == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Client is not connected"));
        }
        
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        clientChannel.writeAndFlush(message).addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                future.complete(null);
                log.debug("Message sent successfully: {}", message.getMessageType());
            } else {
                future.completeExceptionally(channelFuture.cause());
                log.error("Failed to send message: {}", message.getMessageType(), channelFuture.cause());
            }
        });
        
        return future;
    }

    /**
     * 适配 Netty MQTT 消息的发送
     */
    public CompletableFuture<Void> sendMessage(io.netty.handler.codec.mqtt.MqttMessage nettyMessage) {
        org.itsuka.mqtt.protocol.MqttMessage custom = convertFromNetty(nettyMessage);
        return sendMessage(custom);
    }

    private org.itsuka.mqtt.protocol.MqttMessage convertFromNetty(io.netty.handler.codec.mqtt.MqttMessage nettyMessage) {
        io.netty.handler.codec.mqtt.MqttFixedHeader nfh = nettyMessage.fixedHeader();
        org.itsuka.mqtt.protocol.MqttFixedHeader fixedHeader = new org.itsuka.mqtt.protocol.MqttFixedHeader(
                org.itsuka.mqtt.protocol.MqttMessageType.valueOf(nfh.messageType().value()),
                nfh.isDup(),
                org.itsuka.mqtt.protocol.MqttQoS.valueOf(nfh.qosLevel().value()),
                nfh.isRetain(),
                nfh.remainingLength()
        );

        Object variableHeader = null;
        Object payload = null;

        switch (nfh.messageType()) {
            case PUBLISH: {
                io.netty.handler.codec.mqtt.MqttPublishMessage pm = (io.netty.handler.codec.mqtt.MqttPublishMessage) nettyMessage;
                io.netty.handler.codec.mqtt.MqttPublishVariableHeader vh = pm.variableHeader();
                variableHeader = java.util.Map.of(
                        "topic", vh.topicName(),
                        "messageId", vh.packetId()
                );
                io.netty.buffer.ByteBuf buf = pm.payload();
                byte[] bytes = new byte[buf.readableBytes()];
                buf.getBytes(buf.readerIndex(), bytes);
                payload = bytes;
                break;
            }
            case SUBSCRIBE: {
                io.netty.handler.codec.mqtt.MqttSubscribeMessage sm = (io.netty.handler.codec.mqtt.MqttSubscribeMessage) nettyMessage;
                io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader vh = (io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader) sm.variableHeader();
                variableHeader = vh.messageId();
                java.util.List<String> topics = new java.util.ArrayList<>();
                for (io.netty.handler.codec.mqtt.MqttTopicSubscription ts : sm.payload().topicSubscriptions()) {
                    topics.add(ts.topicFilter());
                }
                payload = topics;
                break;
            }
            default: {
                break;
            }
        }

        return new org.itsuka.mqtt.protocol.MqttMessage(fixedHeader, variableHeader, payload);
    }
    
    /**
     * 安排重连
     */
    private void scheduleReconnect() {
        if (!properties.isAutoReconnect()) {
            return;
        }
        
        workerGroup.schedule(() -> {
            if (!connected) {
                log.info("Attempting to reconnect to MQTT server...");
                try {
                    doConnect();
                } catch (Exception e) {
                    log.error("Reconnection failed", e);
                    scheduleReconnect(); // 继续尝试重连
                }
            }
        }, properties.getReconnectInterval(), TimeUnit.SECONDS);
    }
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return connected && clientChannel != null && clientChannel.isActive();
    }
    
    /**
     * 获取客户端ID
     */
    public String getClientId() {
        return properties.getClientId();
    }
}