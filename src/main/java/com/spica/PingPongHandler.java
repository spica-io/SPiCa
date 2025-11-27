package com.spica;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

class PingPongHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final String msg) {
        System.out.println("Received: " + msg);

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