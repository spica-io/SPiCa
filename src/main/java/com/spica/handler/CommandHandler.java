package com.spica.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class CommandHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);
    private final PingPongHandler pingPongHandler;
    private final SetHandler setHandler;
    private final GetHandler getHandler;

    public CommandHandler(PingPongHandler pingPongHandler, SetHandler setHandler, GetHandler getHandler) {
        this.pingPongHandler = pingPongHandler;
        this.setHandler = setHandler;
        this.getHandler = getHandler;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final String msg) throws Exception {
        final String trimmedMsg = msg.trim();

        if (trimmedMsg.isBlank()) {
            ctx.writeAndFlush("비어있습니다.\n");
            return;
        }

        if ("PING".equalsIgnoreCase(trimmedMsg)) {
            pingPongHandler.handle(ctx, trimmedMsg);
            return;
        }

        if ("SET".equalsIgnoreCase(trimmedMsg.split("\\s+")[0])) {
            setHandler.handle(ctx, msg);
            return;
        }

        if ("GET".equalsIgnoreCase(trimmedMsg.split("\\s+")[0])) {
            getHandler.handle(ctx, msg);
            return;
        }
        ctx.writeAndFlush("Unknown command: " + msg + "\n");
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        log.error("Exception caught in CommandHandler", cause);
        ctx.close();
    }
}
