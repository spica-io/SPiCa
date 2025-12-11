package com.spica.handler;

public class SleepHandler {

    public void handle(final String msg) throws InterruptedException {
        final long time = Long.parseLong(msg);
        Thread.sleep(time * 1000);
    }
}
