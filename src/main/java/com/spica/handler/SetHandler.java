package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SetHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(SetHandler.class);
    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final String msg) {
        log.info("Received: '%s'".formatted(msg));
        if (msg.isBlank()) {
            ctx.writeAndFlush("비어있습니다.\n");
            return;
        }
        final String[] input = msg.trim().split("\\s+");
        if (input.length != 3) {
            ctx.writeAndFlush("파라미터 개수는 3개여야 합니다. 입력된 파라미터 수: " + input.length + "\n");
            return;
        }
        final String command = input[0];
        final String key = input[1];
        final String value = input[2];

        if (!"SET".equalsIgnoreCase(command)) {
            ctx.writeAndFlush("Unknown command: " + msg + "\n");
            return;
        }
        if (store.putIfAbsent(key, value) != null) {
            ctx.writeAndFlush("중복 key: " + key + "\n");
            return;
        }
        ctx.writeAndFlush("OK\n");
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        log.error("Exception caught in SetHandler", cause);
        ctx.close();
    }
}
