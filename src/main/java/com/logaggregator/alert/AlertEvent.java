package com.logaggregator.alert;

import com.logaggregator.core.LogEntry;
import java.time.LocalDateTime;

public class AlertEvent {
    private final AlertRule rule;
    private final LogEntry triggeringEntry;
    private final LocalDateTime timestamp;
    private final String message;

    public AlertEvent(AlertRule rule, LogEntry triggeringEntry, LocalDateTime timestamp, String message) {
        this.rule = rule;
        this.triggeringEntry = triggeringEntry;
        this.timestamp = timestamp;
        this.message = message;
    }

    // Getters
    public AlertRule getRule() { return rule; }
    public LogEntry getTriggeringEntry() { return triggeringEntry; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return String.format("AlertEvent{rule=%s, time=%s, message='%s'}",
                rule.getName(), timestamp, message);
    }
}