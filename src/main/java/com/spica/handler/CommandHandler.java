package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);
    private final PingPongHandler pingPongHandler;
    private final SetHandler setHandler;

    public CommandHandler(PingPongHandler pingPongHandler, SetHandler setHandler) {
        this.pingPongHandler = pingPongHandler;
        this.setHandler = setHandler;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final String msg) throws Exception {
        if ("PING".equalsIgnoreCase(msg)) {
            pingPongHandler.channelRead0(ctx, msg);
            return;
        }

        if ("SET".equalsIgnoreCase(msg.split("\\s+")[0])) {
            setHandler.channelRead0(ctx, msg);
            return;
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        log.error("Exception caught in CommandHandler", cause);
        ctx.close();
    }
}
