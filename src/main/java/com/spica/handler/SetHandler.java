package com.spica.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SetHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger log = LoggerFactory.getLogger(SetHandler.class);
    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final String msg) {
        // SET : 사용자에게 {key}와 {value}를 받아 저장하는 명령어이다. 예) SET 김승원 윤지원
        final String[] input = msg.split(" "); // ["SET", "김승원", "윤지원"]
        final String command = input[0];   // "SET"
        final String key = input[1];       // "김승원"
        final String value = input[2];     // "윤지원"

        if ("SET".equalsIgnoreCase(command)) {
            // store에 저장 수행!
            store.put(key, value);
            ctx.writeAndFlush("OK\n");
        } else {
            ctx.writeAndFlush("Unknown command: " + msg + "\n");
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
