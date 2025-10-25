package org.itsuka.mqtt.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.auth.MqttAuthenticator;
import org.itsuka.mqtt.codec.MqttDecoder;
import org.itsuka.mqtt.codec.MqttEncoder;
import org.itsuka.mqtt.event.MqttEventPublisher;
import org.itsuka.mqtt.handler.MqttServerHandler;
import org.itsuka.mqtt.security.MqttRateLimiter;
import org.itsuka.mqtt.session.MqttSessionManager;
import org.itsuka.mqtt.topic.MqttTopicManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.TimeUnit;

/**
 * MQTT服务器
 * 
 * @author itsuka
 */
@Slf4j
public class MqttServer implements InitializingBean, DisposableBean {
    
    private final MqttServerProperties properties;
    private final MqttSessionManager sessionManager;
    private final MqttEventPublisher eventPublisher;
    private final MqttRateLimiter rateLimiter;
    private final MqttTopicManager topicManager;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile int currentConnections = 0;
    
    private MqttAuthenticator authenticator;
    
    public MqttServer(MqttServerProperties properties, 
                     MqttSessionManager sessionManager,
                     MqttEventPublisher eventPublisher,
                     MqttRateLimiter rateLimiter,
                     MqttTopicManager topicManager) {
        this.properties = properties;
        this.sessionManager = sessionManager;
        this.eventPublisher = eventPublisher;
        this.rateLimiter = rateLimiter;
        this.topicManager = topicManager;
        this.authenticator = null;
    }
    
    public MqttServer(MqttServerProperties properties, 
                     MqttSessionManager sessionManager,
                     MqttEventPublisher eventPublisher,
                     MqttRateLimiter rateLimiter,
                     MqttTopicManager topicManager,
                     MqttAuthenticator authenticator) {
        this.properties = properties;
        this.sessionManager = sessionManager;
        this.eventPublisher = eventPublisher;
        this.rateLimiter = rateLimiter;
        this.topicManager = topicManager;
        this.authenticator = authenticator;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        if (properties.isEnabled()) {
            start();
        }
    }
    
    @Override
    public void destroy() throws Exception {
        stop();
    }
    
    /**
     * 启动MQTT服务器
     */
    public java.util.concurrent.CompletableFuture<Void> start() {
        log.info("Starting MQTT server on port {}", properties.getPort());
        
        bossGroup = new NioEventLoopGroup(properties.getBossThreads());
        workerGroup = new NioEventLoopGroup(properties.getWorkerThreads());
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, properties.getBacklog())
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 检查连接数限制
                            if (currentConnections >= properties.getMaxConnections()) {
                                log.warn("Connection limit exceeded: {}/{}", currentConnections, properties.getMaxConnections());
                                ch.close();
                                return;
                            }
                            currentConnections++;
                            
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // 空闲状态处理器
                            pipeline.addLast("idleStateHandler", new IdleStateHandler(
                                    properties.getReaderIdleTime(),
                                    properties.getWriterIdleTime(),
                                    properties.getAllIdleTime(),
                                    TimeUnit.SECONDS
                            ));
                            
                            // MQTT编解码器
                            pipeline.addLast("mqttDecoder", new MqttDecoder());
                            pipeline.addLast("mqttEncoder", new MqttEncoder());
                            
                            // MQTT业务处理器
                            pipeline.addLast("mqttServerHandler", new MqttServerHandler(
                                sessionManager, eventPublisher, rateLimiter, topicManager, authenticator));
                            
                            // 添加连接关闭监听器
                            ch.closeFuture().addListener(future -> currentConnections--);
                        }
                    });
            
            // 绑定端口并启动服务器
            ChannelFuture future = bootstrap.bind(properties.getPort());
            java.util.concurrent.CompletableFuture<Void> cf = new java.util.concurrent.CompletableFuture<>();
            future.addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    serverChannel = f.channel();
                    log.info("MQTT server started successfully on port {}", properties.getPort());
                    cf.complete(null);
                    // 等待服务器关闭
                    f.channel().closeFuture().addListener((ChannelFutureListener) channelFuture -> {
                        log.info("MQTT server stopped");
                    });
                } else {
                    cf.completeExceptionally(f.cause());
                }
            });
            return cf;
            
        } catch (Exception e) {
            log.error("Failed to start MQTT server", e);
            stop();
            java.util.concurrent.CompletableFuture<Void> cf = new java.util.concurrent.CompletableFuture<>();
            cf.completeExceptionally(e);
            return cf;
        }
    }
    
    /**
     * 停止MQTT服务器
     */
    public java.util.concurrent.CompletableFuture<Void> stop() {
        if (serverChannel == null || !serverChannel.isActive()) {
            log.debug("MQTT server is already stopped");
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        
        log.info("Stopping MQTT server...");
        java.util.concurrent.CompletableFuture<Void> cf = new java.util.concurrent.CompletableFuture<>();
        
        serverChannel.close().addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                log.info("MQTT server channel closed");
                
                // 异步关闭事件循环组
                java.util.concurrent.CompletableFuture<Void> bossShutdown = new java.util.concurrent.CompletableFuture<>();
                java.util.concurrent.CompletableFuture<Void> workerShutdown = new java.util.concurrent.CompletableFuture<>();
                
                if (bossGroup != null) {
                    bossGroup.shutdownGracefully().addListener(future -> {
                        if (future.isSuccess()) {
                            log.debug("Boss group shutdown successfully");
                            bossShutdown.complete(null);
                        } else {
                            bossShutdown.completeExceptionally(future.cause());
                        }
                    });
                } else {
                    bossShutdown.complete(null);
                }
                
                if (workerGroup != null) {
                    workerGroup.shutdownGracefully().addListener(future -> {
                        if (future.isSuccess()) {
                            log.debug("Worker group shutdown successfully");
                            workerShutdown.complete(null);
                        } else {
                            workerShutdown.completeExceptionally(future.cause());
                        }
                    });
                } else {
                    workerShutdown.complete(null);
                }
                
                // 等待两个组都关闭完成
                java.util.concurrent.CompletableFuture.allOf(bossShutdown, workerShutdown)
                    .thenRun(() -> {
                        log.info("MQTT server stopped successfully");
                        cf.complete(null);
                    })
                    .exceptionally(throwable -> {
                        log.error("Failed to shutdown MQTT server gracefully", throwable);
                        cf.completeExceptionally(throwable);
                        return null;
                    });
            } else {
                log.error("Failed to close MQTT server channel", f.cause());
                cf.completeExceptionally(f.cause());
            }
        });
        
        return cf;
    }
    
    /**
     * 检查服务器是否正在运行
     */
    public boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }
    
    /**
     * 获取服务器端口
     */
    public int getPort() {
        return properties.getPort();
    }
}