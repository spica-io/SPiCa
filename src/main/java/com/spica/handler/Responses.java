package com.spica.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;

public final class Responses {

    private Responses() {
    } // 유틸리티 클래스

    // ===== 상수 응답 (미리 할당, 재사용) =====

    public static final ByteBuf OK = constant("OK\n");
    public static final ByteBuf PONG = constant("Pong\n");
    public static final ByteBuf EMPTY_INPUT = constant("비어있습니다.\n");
    public static final ByteBuf KEY_NOT_FOUND = constant("존재하지 않는 key입니다.\n");

    // 에러 메시지
    public static final ByteBuf SET_USAGE = constant(
            "SET 명령어 구문이 올바르지 않습니다. 사용법: SET <key> <value> 또는 SET <key> <newValue> MATCH <oldValue>\n");
    public static final ByteBuf GET_USAGE = constant(
            "GET 명령어 구문이 올바르지 않습니다. 사용법: GET <key>\n");
    public static final ByteBuf MGET_USAGE = constant(
            "MGET 명령어 구문이 올바르지 않습니다. 사용법: MGET <key> <key> ...\n");
    public static final ByteBuf DEL_USAGE = constant(
            "DEL 명령어 구문이 올바르지 않습니다. 사용법: DEL <key>\n");

    // ===== 전송 메서드 =====

    /**
     * 상수 ByteBuf 전송 (Zero-Copy, 재사용)
     */
    public static void send(final ChannelHandlerContext ctx, final ByteBuf constant) {
        ctx.writeAndFlush(constant.retainedDuplicate());
    }

    /**
     * OK 응답 전송
     */
    public static void ok(final ChannelHandlerContext ctx) {
        send(ctx, OK);
    }

    /**
     * PONG 응답 전송
     */
    public static void pong(final ChannelHandlerContext ctx) {
        send(ctx, PONG);
    }

    /**
     * 키 없음 응답 전송
     */
    public static void keyNotFound(final ChannelHandlerContext ctx) {
        send(ctx, KEY_NOT_FOUND);
    }

    /**
     * 동적 문자열 응답 전송 (Pooled Allocator 사용)
     */
    public static void send(final ChannelHandlerContext ctx, final String message) {
        final byte[] bytes = message.getBytes(CharsetUtil.UTF_8);
        final ByteBuf buf = ctx.alloc().buffer(bytes.length);
        buf.writeBytes(bytes);
        ctx.writeAndFlush(buf);
    }

    /**
     * 포맷된 동적 응답 전송
     */
    public static void sendFormat(final ChannelHandlerContext ctx, final String format, final Object... args) {
        send(ctx, String.format(format, args));
    }

    // ===== 내부 헬퍼 =====

    private static ByteBuf constant(final String content) {
        return Unpooled.unreleasableBuffer(
                Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
    }
}
