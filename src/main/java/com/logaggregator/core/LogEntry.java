package com.logaggregator.core;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


public class LogEntry {
    private final String id;
    private final String source;
    private final String rawMessage;
    private final LogLevel level;
    private final LocalDateTime timestamp;
    private final String message;
    private final Map<String, Object> fields;

    public LogEntry(String source, String rawMessage, LogLevel level,
            LocalDateTime timestamp, String message) {
        this.id = generateId();
        this.source = source;
        this.rawMessage = rawMessage;
        this.level = level;
        this.timestamp = timestamp;
        this.message = message;
        this.fields = new HashMap<>();
    }

    private String generateId() {
        return java.util.UUID.randomUUID().toString();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public LogLevel getLevel() {
        return level;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void addField(String key, Object value) {
        fields.put(key, value);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %s: %s",
                timestamp, level, source, message);
    }
}
