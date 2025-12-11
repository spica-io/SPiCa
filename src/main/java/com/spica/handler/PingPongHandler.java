package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingPongHandler {
    private static final Logger log = LoggerFactory.getLogger(PingPongHandler.class);

    void handle(final ChannelHandlerContext ctx) {
        ctx.writeAndFlush("Pong\n");
    }
}
