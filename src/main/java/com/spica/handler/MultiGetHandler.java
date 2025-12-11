package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

public class MultiGetHandler {
    private final Map<String, String> store;

    public MultiGetHandler(Map<String, String> store) {
        this.store = store;
    }

    void handle(final ChannelHandlerContext ctx, final String[] input) {
        final String command = input[0];
        final int keyCount = input.length - 1;
        final StringBuilder responseBuilder = new StringBuilder();

        for (int i = 1; i <= keyCount; i++) {
            final String key = input[i];
            final String value = store.getOrDefault(key, null);

            if (value == null) {
                responseBuilder.append("존재하지 않는 key: ").append(key).append("\n");
            } else {
                responseBuilder.append(value).append("\n");
            }
        }
        ctx.writeAndFlush(responseBuilder.toString());
    }
}