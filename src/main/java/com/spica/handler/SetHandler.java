package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SetHandler {
    private final Map<String, String> store;

    public SetHandler(final Map<String, String> store) {
        this.store = store;
    }

    void setIfAbsent(final ChannelHandlerContext ctx, final String[] input) {

        final String command = input[0];
        final String key = input[1];
        final String value = input[2];

        if (store.putIfAbsent(key, value) != null) {
            ctx.writeAndFlush("중복 key: " + key + "\n");
            return;
        }
        ctx.writeAndFlush("OK\n");
    }

    void setIfMatches(final ChannelHandlerContext ctx, final String[] input) {
        final String command = input[0];
        final String key = input[1];
        final String newValue = input[2];
        final String oldValue = input[4];

        if (store.replace(key, oldValue, newValue)){
            ctx.writeAndFlush("OK\n");
        } else {
            final String currentValue = store.getOrDefault(key, null);
            if (currentValue == null) {
                ctx.writeAndFlush("존재하지 않는 key: " + key + "\n");
            } else {
                ctx.writeAndFlush("올바르지 않은 이전 값: " + oldValue + "\n");
            }
        }
    }
}
