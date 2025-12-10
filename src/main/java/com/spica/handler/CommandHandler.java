package com.spica.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

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

        switch (command) {
            case "PING":
                pingPongHandler.handle(ctx, command);
                return;

            case "SET":
                final String[] input = msg.trim().split("\\s+");
                if (input.length == 5 && msg.trim().split("\\s+")[3].equalsIgnoreCase("MATCH")) {
                    setHandler.setIfMatches(ctx, msg);
                    return;
                }
                setHandler.setIfAbsent(ctx, msg);
                return;

            case "GET":
                getHandler.handle(ctx, msg);
                return;

            case "DEL":
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
