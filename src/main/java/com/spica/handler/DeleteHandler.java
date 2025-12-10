package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DeleteHandler {
    private static final Logger log = LoggerFactory.getLogger(SetHandler.class);
    private final Map<String, String> store;

    public DeleteHandler(Map<String, String> store) {
        this.store = store;
    }

    void handle(final ChannelHandlerContext ctx, final String msg){
        log.info("Received: '%s'".formatted(msg));
        final String input[] = msg.trim().split("\\s+");

        final String command = input[0];
        final String key = input[1];

        store.remove(key);

        ctx.writeAndFlush("OK");
    }
}
