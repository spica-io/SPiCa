package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class CommandHandler extends SimpleChannelInboundHandler<String> {
    private static final Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final String msg) throws Exception {
    }
}
