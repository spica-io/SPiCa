package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;

import java.util.Map;

import static com.spica.handler.Responses.*;

public final class SetHandler {

    private final Map<String, byte[]> store;

    public SetHandler(final Map<String, byte[]> store) {
        this.store = store;
    }

    void setIfAbsent(final ChannelHandlerContext ctx, final String[] args) {
        final String key = args[1];
        final String value = args[2];
        final byte[] valueBytes = value.getBytes(CharsetUtil.UTF_8);

        if (store.putIfAbsent(key, valueBytes) != null) {
            send(ctx, "중복 key: " + key + "\n");
            return;
        }

        ok(ctx);
    }

    void setIfMatches(final ChannelHandlerContext ctx, final String[] args) {
        final String key = args[1];
        final String newValue = args[2];
        final String expectedOldValue = args[4];

        final byte[] currentBytes = store.get(key);

        if (currentBytes == null) {
            send(ctx, "존재하지 않는 key: " + key + "\n");
            return;
        }

        final String currentValue = new String(currentBytes, CharsetUtil.UTF_8);

        if (currentValue.equals(expectedOldValue)) {
            store.put(key, newValue.getBytes(CharsetUtil.UTF_8));
            ok(ctx);
        } else {
            send(ctx, "올바르지 않은 이전 값: " + expectedOldValue + "\n");
        }
    }
}
