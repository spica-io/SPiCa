package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

import static com.spica.handler.Responses.*;

public final class DeleteHandler {

    private final Map<String, byte[]> store;

    public DeleteHandler(final Map<String, byte[]> store) {
        this.store = store;
    }

    void handle(final ChannelHandlerContext ctx, final String[] args) {
        final String key = args[1];

        if (store.remove(key) == null) {
            keyNotFound(ctx);
            return;
        }

        ok(ctx);
    }
}
