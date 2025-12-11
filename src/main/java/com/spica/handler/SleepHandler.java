package com.spica.handler;

public class SleepHandler {

    public void handle(final String[] input) throws InterruptedException {
        final long time = Long.parseLong(input[1]);
        Thread.sleep(time * 1000);
    }
}
