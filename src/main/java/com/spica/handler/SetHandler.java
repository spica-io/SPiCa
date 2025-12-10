package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SetHandler {
    private static final Logger log = LoggerFactory.getLogger(SetHandler.class);
    private final Map<String, String> store;

    public SetHandler(final Map<String, String> store) {
        this.store = store;
    }

    void setIfAbsent(final ChannelHandlerContext ctx, final String msg) {
        log.info("Received: '%s'".formatted(msg));
        final String[] input = msg.trim().split("\\s+");

        if (input.length != 3) {
            ctx.writeAndFlush("SET시 파라미터 개수는 3개 또는 5개여야 합니다. 입력된 파라미터 수: " + input.length + "\n");
            return;
        }
        final String command = input[0];
        final String key = input[1];
        final String value = input[2];

        if (store.putIfAbsent(key, value) != null) {
            ctx.writeAndFlush("중복 key: " + key + "\n");
            return;
        }
        ctx.writeAndFlush("OK\n");
    }

    void setIfMatches(final ChannelHandlerContext ctx, final String msg) {
        log.info("Received: '%s'".formatted(msg));
        final String[] input = msg.trim().split("\\s+");

        final String command = input[0];
        final String key = input[1];
        final String newValue = input[2];
        final String oldValue = input[4];

        final String currentOldValue = store.getOrDefault(key, null);

        store.putIfAbsent(key, newValue);
        ctx.writeAndFlush("OK\n");
    }
}
