package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CommandHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);
    private final Map<String, String> store;

    public CommandHandler(final Map<String, String> store) {
        this.store = store;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final String msg) throws Exception {
        if ("PING".equalsIgnoreCase(msg)) {
            PingPongHandler pingPongHandler = new PingPongHandler();
            pingPongHandler.channelRead0(ctx, msg);
            return;
        }

        if ("SET".equalsIgnoreCase(msg.split("\\s+")[0])) {
            SetHandler setHandler = new SetHandler(store);
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
