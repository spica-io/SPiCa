package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;

public class MultiGetHandler {
    private final Map<String, String> store;

    public MultiGetHandler(Map<String, String> store) {
        this.store = store;
    }

    void handle(final ChannelHandlerContext ctx, final String[] input) {

    }
}