package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GetHandler {
    private final Map<String, String> store;

    public GetHandler(Map<String, String> store) {
        this.store = store;
    }

    void handle(final ChannelHandlerContext ctx, final String[] input){

        final String command = input[0];
        final String key = input[1];
        final String value = store.getOrDefault(key, null);

        if (value == null) {
            ctx.writeAndFlush("존재하지 않는 key입니다. 입력된 key: " + key + "\n");
            return;
        }

        ctx.writeAndFlush("value: " + value + "\n");
    }
}
