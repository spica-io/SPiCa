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
    private final DeleteHandler deleteHandler;

    public CommandHandler(final PingPongHandler pingPongHandler, final SetHandler setHandler, final GetHandler getHandler, final DeleteHandler deleteHandler) {
        this.pingPongHandler = pingPongHandler;
        this.setHandler = setHandler;
        this.getHandler = getHandler;
        this.deleteHandler = deleteHandler;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final String msg) throws Exception {
        final String command = msg.trim().split("\\s+")[0];

        if (command.isBlank()) {
            ctx.writeAndFlush("비어있습니다.\n");
            return;
        }

        if ("PING".equalsIgnoreCase(command)) {
            pingPongHandler.handle(ctx, command);
            return;
        }

        if ("SET".equalsIgnoreCase(command)) {
            setHandler.handle(ctx, msg);
            return;
        }

        if ("GET".equalsIgnoreCase(command)) {
            getHandler.handle(ctx, msg);
            return;
        }

        if ("DEL".equalsIgnoreCase(command)) {
            deleteHandler.handle(ctx, msg);
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
