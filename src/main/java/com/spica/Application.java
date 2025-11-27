package com.spica;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(final String[] args) {
        final ServerConfiguration config = new ServerConfiguration(6379, 1, 0, 1024);
        final Server server = new NettyServer(config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook triggered. Stopping server.");
            server.stop();
        }));

        try {
            server.start();
            server.blockUntilClose();
        } catch (final InterruptedException e) {
            log.error("Server start was interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}