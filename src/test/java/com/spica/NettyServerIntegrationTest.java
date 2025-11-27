package com.spica;

import com.spica.server.NettyServer;
import com.spica.server.Server;
import com.spica.server.ServerConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class NettyServerIntegrationTest {
    private static final int TEST_PORT = 7777;
    private static final String HOST = "localhost";
    private Server server;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws InterruptedException {
        // Given: Start server in a separate thread
        ServerConfiguration config = new ServerConfiguration(TEST_PORT, 1, 2, 1024);
        server = new NettyServer(config);

        CountDownLatch serverStarted = new CountDownLatch(1);
        serverThread = new Thread(() -> {
            try {
                server.start();
                serverStarted.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        serverThread.start();

        // Wait for server to start
        assertTrue(serverStarted.await(5, TimeUnit.SECONDS), "Server should start within 5 seconds");
        Thread.sleep(500); // Give server additional time to bind
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    @Test
    @DisplayName("클라이언트가 Ping을 보내면 Pong으로 응답해야 한다")
    void testPingPongResponse() throws IOException {
        try (Socket socket = new Socket(HOST, TEST_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // When
            out.println("Ping");

            // Then
            String response = in.readLine();
            assertNotNull(response);
            assertEquals("Pong", response);
        }
    }

    @Test
    @DisplayName("같은 클라이언트로부터의 여러 ping 요청을 처리할 수 있어야 한다")
    void testMultiplePingRequests() throws IOException {
        try (Socket socket = new Socket(HOST, TEST_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // When & Then
            for (int i = 0; i < 5; i++) {
                out.println("Ping");
                String response = in.readLine();
                assertEquals("Pong", response, "Response " + i + " should be Pong");
            }
        }
    }

    @Test
    @DisplayName("대소문자 구분 없이 ping 명령어를 처리할 수 있어야 한다")
    void testCaseInsensitivePing() throws IOException {
        try (Socket socket = new Socket(HOST, TEST_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // When & Then
            out.println("ping");
            assertEquals("Pong", in.readLine());

            out.println("PING");
            assertEquals("Pong", in.readLine());

            out.println("PiNg");
            assertEquals("Pong", in.readLine());
        }
    }

    @Test
    @DisplayName("알 수 없는 명령어에 대해 에러 메시지로 응답해야 한다")
    void testUnknownCommand() throws IOException {
        try (Socket socket = new Socket(HOST, TEST_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // When
            out.println("Hello");

            // Then
            String response = in.readLine();
            assertNotNull(response);
            assertTrue(response.contains("Unknown command"));
            assertTrue(response.contains("Hello"));
        }
    }

    @Test
    @DisplayName("여러 클라이언트의 동시 연결을 처리할 수 있어야 한다")
    void testConcurrentConnections() throws InterruptedException {
        int numberOfClients = 10;
        CountDownLatch latch = new CountDownLatch(numberOfClients);

        for (int i = 0; i < numberOfClients; i++) {
            new Thread(() -> {
                try (Socket socket = new Socket(HOST, TEST_PORT);
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    out.println("Ping");
                    String response = in.readLine();
                    assertEquals("Pong", response);

                } catch (IOException e) {
                    fail("Client connection failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "All clients should complete within 10 seconds");
    }

    @Test
    @DisplayName("유효한 명령어와 무효한 명령어가 섞여있어도 처리할 수 있어야 한다")
    void testMixedCommands() throws IOException {
        try (Socket socket = new Socket(HOST, TEST_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // When & Then
            out.println("Ping");
            assertEquals("Pong", in.readLine());

            out.println("InvalidCommand");
            String response = in.readLine();
            assertTrue(response.contains("Unknown command"));

            out.println("ping");
            assertEquals("Pong", in.readLine());
        }
    }

    @Test
    @DisplayName("빠른 연속 요청을 처리할 수 있어야 한다")
    void testRapidRequests() throws IOException {
        try (Socket socket = new Socket(HOST, TEST_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // When & Then
            for (int i = 0; i < 100; i++) {
                out.println("Ping");
                String response = in.readLine();
                assertEquals("Pong", response, "Request " + i + " failed");
            }
        }
    }
}
