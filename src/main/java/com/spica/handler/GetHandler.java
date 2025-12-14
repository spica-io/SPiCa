package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;

import java.util.Map;

import static com.spica.handler.Responses.*;

public final class GetHandler {

    private final Map<String, byte[]> store;

    public GetHandler(final Map<String, byte[]> store) {
        this.store = store;
    }

    void handle(final ChannelHandlerContext ctx, final String[] args) {
        final String key = args[1];
        final byte[] valueBytes = store.get(key);

        if (valueBytes == null) {
            send(ctx, "존재하지 않는 key입니다. 입력된 key: " + key + "\n");
            return;
        }

        final String value = new String(valueBytes, CharsetUtil.UTF_8);
        send(ctx, "value: " + value + "\n");
    }
}
