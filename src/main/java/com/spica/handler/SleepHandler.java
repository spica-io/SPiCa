package com.spica.handler;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class SleepHandler {

    void handle(final String[] input) throws InterruptedException {
        final long time = Long.parseLong(input[1]);
        Thread.sleep(Duration.of(time, ChronoUnit.SECONDS));
    }
}
