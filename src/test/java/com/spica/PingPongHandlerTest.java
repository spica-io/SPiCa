package com.spica;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PingPongHandlerTest {
    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        channel = new EmbeddedChannel(new PingPongHandler());
    }

    @Test
    @DisplayName("'Ping'을 받으면 'Pong'으로 응답해야 한다")
    void testPingCommand() {
        // When
        channel.writeInbound("Ping");
        channel.finish();

        // Then
        String response = channel.readOutbound();
        assertNotNull(response);
        assertEquals("Pong\n", response);
    }

    @Test
    @DisplayName("'ping'(소문자)을 받으면 'Pong'으로 응답해야 한다")
    void testPingCommandLowercase() {
        // When
        channel.writeInbound("ping");
        channel.finish();

        // Then
        String response = channel.readOutbound();
        assertNotNull(response);
        assertEquals("Pong\n", response);
    }

    @Test
    @DisplayName("'PING'(대문자)을 받으면 'Pong'으로 응답해야 한다")
    void testPingCommandUppercase() {
        // When
        channel.writeInbound("PING");
        channel.finish();

        // Then
        String response = channel.readOutbound();
        assertNotNull(response);
        assertEquals("Pong\n", response);
    }

    @Test
    @DisplayName("알 수 없는 명령어를 받으면 에러 메시지로 응답해야 한다")
    void testUnknownCommand() {
        // When
        channel.writeInbound("Hello");
        channel.finish();

        // Then
        String response = channel.readOutbound();
        assertNotNull(response);
        assertEquals("Unknown command: Hello\n", response);
    }

    @Test
    @DisplayName("여러 개의 ping 명령어를 처리할 수 있어야 한다")
    void testMultiplePingCommands() {
        // When
        channel.writeInbound("Ping");
        channel.writeInbound("ping");
        channel.writeInbound("PING");
        channel.finish();

        // Then
        assertEquals("Pong\n", channel.readOutbound());
        assertEquals("Pong\n", channel.readOutbound());
        assertEquals("Pong\n", channel.readOutbound());
    }

    @Test
    @DisplayName("유효한 명령어와 무효한 명령어가 섞여있어도 처리할 수 있어야 한다")
    void testMixedCommands() {
        // When
        channel.writeInbound("Ping");
        channel.writeInbound("Invalid");
        channel.writeInbound("ping");
        channel.finish();

        // Then
        assertEquals("Pong\n", channel.readOutbound());
        assertEquals("Unknown command: Invalid\n", channel.readOutbound());
        assertEquals("Pong\n", channel.readOutbound());
    }

    @Test
    @DisplayName("빈 문자열을 처리할 수 있어야 한다")
    void testEmptyString() {
        // When
        channel.writeInbound("");
        channel.finish();

        // Then
        String response = channel.readOutbound();
        assertNotNull(response);
        assertEquals("Unknown command: \n", response);
    }
}
