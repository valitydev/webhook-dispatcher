package dev.vality.webhook.dispatcher.utils;

import java.time.Instant;

public class TimeoutUtils {

    public static long calculateTimeFromCreated(String createdAt) {
        return Instant.now().toEpochMilli() - Instant.parse(createdAt).toEpochMilli();
    }

}
