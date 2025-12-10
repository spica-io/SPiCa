package com.spica.server;

import com.spica.handler.CommandHandler;
import com.spica.handler.PingPongHandler;
import com.spica.handler.SetHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NettyServer implements Server {
    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);
    private final ServerConfiguration config;
    private final Map<String, String> store = new ConcurrentHashMap<>();

    private final PingPongHandler pingPongHandler = new PingPongHandler();
    private final SetHandler setHandler = new SetHandler(store);
    private final CommandHandler commandHandler = new CommandHandler(pingPongHandler, setHandler);
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    public NettyServer(final ServerConfiguration config) {
        this.config = config;
    }

    @Override
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(config.bossThreads());
        workerGroup = new NioEventLoopGroup(config.workerThreads());

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new LineBasedFrameDecoder(config.maxFrameLength()));
                        ch.pipeline().addLast(new StringDecoder(CharsetUtil.UTF_8));
                        ch.pipeline().addLast(new StringEncoder(CharsetUtil.UTF_8));
                        ch.pipeline().addLast(commandHandler);
                    }
                });

        channelFuture = b.bind(config.port()).sync();
        log.info("Server started and bound to port {}", config.port());
    }

    @Override
    public void stop() {
        log.info("Stopping server.");
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void blockUntilClose() throws InterruptedException {
        if (channelFuture != null) {
            channelFuture.channel().closeFuture().sync();
        }
    }
}