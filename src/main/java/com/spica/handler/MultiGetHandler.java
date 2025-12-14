package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;

import java.util.Map;

import static com.spica.handler.Responses.*;

public final class MultiGetHandler {

    private static final int ESTIMATED_VALUE_SIZE = 32;

    private final Map<String, byte[]> store;

    public MultiGetHandler(final Map<String, byte[]> store) {
        this.store = store;
    }

    void handle(final ChannelHandlerContext ctx, final String[] args) {
        final int keyCount = args.length - 1;
        final StringBuilder response = new StringBuilder(keyCount * ESTIMATED_VALUE_SIZE);

        for (int i = 1; i <= keyCount; i++) {
            final String key = args[i];
            final byte[] valueBytes = store.get(key);

            if (valueBytes == null) {
                response.append("존재하지 않는 key: ").append(key).append('\n');
            } else {
                response.append(new String(valueBytes, CharsetUtil.UTF_8)).append('\n');
            }
        }

        send(ctx, response.toString());
    }
}