package dev.vality.webhook.dispatcher.utils;

public class IdGenerator {

    private static final String DELIMITER = "_";

    public static String generate(Long hookId, String sourceId, long eventId) {
        return hookId + DELIMITER + sourceId + DELIMITER + eventId;
    }

}
