package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GetHandler {
    private static final Logger log = LoggerFactory.getLogger(GetHandler.class);
    private final Map<String, String> store;

    public GetHandler(Map<String, String> store) {
        this.store = store;
    }

    void handle(final ChannelHandlerContext ctx, final String msg){

    }
}
