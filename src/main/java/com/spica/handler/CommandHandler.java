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
        final String[] input = msg.trim().split("\\s+");
        final String command = input[0].toUpperCase(Locale.ROOT);

        log.info("Received: '%s'".formatted(msg));

        if (command.isBlank()) {
            ctx.writeAndFlush("비어있습니다.\n");
            return;
        }

        switch (command) {
            case "PING":
                pingPongHandler.handle(ctx);
                return;

            case "SET":
                if (input.length == 5 && input[3].equalsIgnoreCase("MATCH")) {
                    setHandler.setIfMatches(ctx, input);
                    return;
                }
                setHandler.setIfAbsent(ctx, input);
                return;

            case "GET":
                getHandler.handle(ctx, input);
                return;

            case "DEL":
                deleteHandler.handle(ctx, input);
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
