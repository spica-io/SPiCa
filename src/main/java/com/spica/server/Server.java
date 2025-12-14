package com.spica.server;

public interface Server {
    void start() throws InterruptedException;

    void stop();

    void blockUntilClose() throws InterruptedException;
}
