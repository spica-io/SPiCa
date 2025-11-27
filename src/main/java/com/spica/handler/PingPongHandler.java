package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingPongHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(PingPongHandler.class);

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final String msg) {
        log.info("Received: " + msg);

        if ("Ping".equalsIgnoreCase(msg)) {
            ctx.writeAndFlush("Pong\n");
        } else {
            ctx.writeAndFlush("Unknown command: " + msg + "\n");
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
