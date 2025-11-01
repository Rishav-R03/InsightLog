package com.logaggregator.alert;

import com.logaggregator.core.LogEntry;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.regex.Pattern;
import java.time.LocalDateTime;

public class AlertRule {
    private final String id;
    private final String name;
    private final Pattern pattern;
    private final String field;
    private final AlertSeverity severity;
    private final String message;
    private volatile LocalDateTime lastTriggered;
    private volatile long triggerCount;

    @JsonCreator
    public AlertRule(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("pattern") String pattern,
            @JsonProperty("field") String field,
            @JsonProperty("severity") AlertSeverity severity,
            @JsonProperty("message") String message) {
        this.id = id != null ? id : java.util.UUID.randomUUID().toString();
        this.name = name;
        this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        this.field = field != null ? field : "message";
        this.severity = severity != null ? severity : AlertSeverity.MEDIUM;
        this.message = message;
        this.triggerCount = 0;
    }

    public boolean matches(LogEntry entry) {
        String textToMatch;

        switch (field) {
            case "message":
                textToMatch = entry.getMessage();
                break;
            case "source":
                textToMatch = entry.getSource();
                break;
            case "level":
                textToMatch = entry.getLevel().name();
                break;
            default:
                Object fieldValue = entry.getFields().get(field);
                textToMatch = fieldValue != null ? fieldValue.toString() : "";
                break;
        }

        if (textToMatch == null) return false;

        return pattern.matcher(textToMatch).find();
    }

    public void trigger(LogEntry entry) {
        this.lastTriggered = LocalDateTime.now();
        this.triggerCount++;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getPattern() { return pattern.pattern(); }
    public String getField() { return field; }
    public AlertSeverity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public LocalDateTime getLastTriggered() { return lastTriggered; }
    public long getTriggerCount() { return triggerCount; }

    @Override
    public String toString() {
        return String.format("AlertRule{name='%s', pattern='%s', severity=%s, triggers=%d}",
                name, pattern.pattern(), severity, triggerCount);
    }
}