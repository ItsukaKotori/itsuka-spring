package org.itsuka.mqtt.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.itsuka.mqtt.codec.MqttDecoder;
import org.itsuka.mqtt.codec.MqttEncoder;
import org.itsuka.mqtt.event.MqttEventPublisher;
import org.itsuka.mqtt.handler.MqttServerHandler;
import org.itsuka.mqtt.session.MqttSessionManager;
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
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    public MqttServer(MqttServerProperties properties, 
                     MqttSessionManager sessionManager,
                     MqttEventPublisher eventPublisher) {
        this.properties = properties;
        this.sessionManager = sessionManager;
        this.eventPublisher = eventPublisher;
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
    public void start() throws InterruptedException {
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
                            pipeline.addLast("mqttServerHandler", new MqttServerHandler(sessionManager, eventPublisher));
                        }
                    });
            
            // 绑定端口并启动服务器
            ChannelFuture future = bootstrap.bind(properties.getPort()).sync();
            serverChannel = future.channel();
            
            log.info("MQTT server started successfully on port {}", properties.getPort());
            
            // 等待服务器关闭
            future.channel().closeFuture().addListener((ChannelFutureListener) channelFuture -> {
                log.info("MQTT server stopped");
            });
            
        } catch (Exception e) {
            log.error("Failed to start MQTT server", e);
            stop();
            throw e;
        }
    }
    
    /**
     * 停止MQTT服务器
     */
    public void stop() {
        log.info("Stopping MQTT server...");
        
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while closing server channel", e);
            Thread.currentThread().interrupt();
        } finally {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
        }
        
        log.info("MQTT server stopped successfully");
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