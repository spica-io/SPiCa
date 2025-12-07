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
        final String[] input = msg.split(" ");
        final String command = input[0];
        final String key = input[1];
        final String value = input[2];

        if (store.containsKey(key)) {
            ctx.writeAndFlush("중복 key: " + key + "\n");
            return;
        }
        if (!"SET".equalsIgnoreCase(command)) {
            ctx.writeAndFlush("Unknown command: " + msg + "\n");
            return;
        }
        store.put(key, value);
        ctx.writeAndFlush("OK\n");
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
