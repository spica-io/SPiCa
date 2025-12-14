package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;

public final class PingPongHandler {

    void handle(final ChannelHandlerContext ctx) {
        // CommandHandler에서 Responses.pong() 직접 호출
    }
}
