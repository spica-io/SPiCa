package com.spica;

record ServerConfiguration(
        int port,
        int bossThreads,
        int workerThreads,
        int maxFrameLength
) {
}
