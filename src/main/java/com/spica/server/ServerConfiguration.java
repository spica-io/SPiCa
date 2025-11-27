package com.spica.server;

public record ServerConfiguration(
        int port,
        int bossThreads,
        int workerThreads,
        int maxFrameLength
) {
}
