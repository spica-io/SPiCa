package com.spica.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.ExecutorService;

import static com.spica.handler.Responses.*;

@ChannelHandler.Sharable
public final class CommandHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);
    private final ExecutorService slowPathExecutor;
    private final PingPongHandler pingPongHandler;
    private final SleepHandler sleepHandler;
    private final SetHandler setHandler;
    private final GetHandler getHandler;
    private final DeleteHandler deleteHandler;
    private final MultiGetHandler multiGetHandler;

    public CommandHandler(
            final ExecutorService slowPathExecutor,
            final PingPongHandler pingPongHandler,
            final SleepHandler sleepHandler,
            final SetHandler setHandler,
            final GetHandler getHandler,
            final MultiGetHandler multiGetHandler,
            final DeleteHandler deleteHandler) {
        this.slowPathExecutor = slowPathExecutor;
        this.pingPongHandler = pingPongHandler;
        this.sleepHandler = sleepHandler;
        this.setHandler = setHandler;
        this.getHandler = getHandler;
        this.multiGetHandler = multiGetHandler;
        this.deleteHandler = deleteHandler;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final ByteBuf msg) throws Exception {
        final String input = parseInput(msg);

        if (input.isEmpty()) {
            send(ctx, EMPTY_INPUT);
            return;
        }

        logIfDebug(input);

        final String command = extractCommand(input);

        dispatch(ctx, command, input);
    }

    private void dispatch(final ChannelHandlerContext ctx, final String command, final String input) {
        switch (command) {
            case "PING" -> pong(ctx);
            case "SLEEP" -> handleSleep(input);
            case "SET" -> handleSet(ctx, input);
            case "GET" -> handleGet(ctx, input);
            case "MGET" -> handleMget(ctx, input);
            case "DEL" -> handleDel(ctx, input);
            default -> send(ctx, "Unknown command: " + extractFirstWord(input) + "\n");
        }
    }

    private void handleSleep(final ChannelHandlerContext ctx, final String input) {
       slowPathExecutor.submit(() -> {
           try {
               final String[] args = input.split("\\s+");
               sleepHandler.handle(args);
               send(ctx, "OK");
           } catch (final Exception e) {
               send(ctx, "ERROR: An internal error occurred while processing the command.\n");
           }
       });
    }

    private void handleSet(final ChannelHandlerContext ctx, final String input) {
        final String[] args = input.split("\\s+");
        if (args.length == 3) {
            setHandler.setIfAbsent(ctx, args);
        } else if (args.length == 5 && args[3].equalsIgnoreCase("MATCH")) {
            setHandler.setIfMatches(ctx, args);
        } else {
            send(ctx, SET_USAGE);
        }
    }

    private void handleGet(final ChannelHandlerContext ctx, final String input) {
        final String[] args = input.split("\\s+");
        if (args.length == 2) {
            getHandler.handle(ctx, args);
        } else {
            send(ctx, GET_USAGE);
        }
    }

    private void handleMget(final ChannelHandlerContext ctx, final String input) {
        final String[] args = input.split("\\s+");
        if (args.length >= 2) {
            multiGetHandler.handle(ctx, args);
        } else {
            send(ctx, MGET_USAGE);
        }
    }

    private void handleDel(final ChannelHandlerContext ctx, final String input) {
        final String[] args = input.split("\\s+");
        if (args.length == 2) {
            deleteHandler.handle(ctx, args);
        } else {
            send(ctx, DEL_USAGE);
        }
    }

    // ===== 유틸리티 메서드 =====

    private String parseInput(final ByteBuf msg) {
        if (msg.readableBytes() == 0) {
            return "";
        }
        return msg.toString(CharsetUtil.UTF_8).trim();
    }

    private String extractCommand(final String input) {
        final int spaceIdx = input.indexOf(' ');
        final String raw = (spaceIdx == -1) ? input : input.substring(0, spaceIdx);
        return raw.toUpperCase(Locale.ROOT);
    }

    private String extractFirstWord(final String input) {
        final int spaceIdx = input.indexOf(' ');
        return (spaceIdx == -1) ? input : input.substring(0, spaceIdx);
    }

    private void logIfDebug(final String input) {
        if (log.isDebugEnabled()) {
            log.debug("Received: '{}'", input);
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        log.error("Exception caught in CommandHandler", cause);
        ctx.close();
    }
}
