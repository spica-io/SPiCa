package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final String msg) throws Exception {

    }
}
