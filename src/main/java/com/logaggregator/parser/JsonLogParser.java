package com.logaggregator.parser;

import com.logaggregator.core.LogEntry;
import com.logaggregator.core.LogLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;


public class JsonLogParser implements LogParser{
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    };

    @Override
    public boolean supports(String logFormat) {
        return "json".equalsIgnoreCase(logFormat);
    }

    @Override
    public Optional<LogEntry> parse(String source, String rawLine) {
        try {
            JsonNode jsonNode = mapper.readTree(rawLine.trim());

            // Extract basic fields
            String timestampStr = getField(jsonNode, "timestamp", "time", "@timestamp");
            String levelStr = getField(jsonNode, "level", "loglevel", "severity");
            String message = getField(jsonNode, "message", "msg", "log");

            if (message == null) {
                return Optional.empty();
            }

            // Parse timestamp
            LocalDateTime timestamp = parseTimestamp(timestampStr);
            if (timestamp == null) {
                timestamp = LocalDateTime.now();
            }

            // Parse log level
            LogLevel level = parseLogLevel(levelStr);

            // Create log entry
            LogEntry entry = new LogEntry(source, rawLine, level, timestamp, message);

            // Add all JSON fields as additional fields
            jsonNode.fields().forEachRemaining(field -> {
                if (!field.getKey().equals("message") && !field.getKey().equals("timestamp")) {
                    entry.addField(field.getKey(), field.getValue().asText());
                }
            });

            return Optional.of(entry);

        } catch (Exception e) {
            // Not a valid JSON line
            return Optional.empty();
        }
    }

    private String getField(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field != null && !field.isNull()) {
                return field.asText();
            }
        }
        return null;
    }

    private LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null) return null;

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(timestampStr, formatter);
            } catch (Exception e) {
                // Try next formatter
            }
        }
        return null;
    }

    private LogLevel parseLogLevel(String levelStr) {
        if (levelStr == null) return LogLevel.INFO;

        try {
            return LogLevel.valueOf(levelStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Fallback to INFO for unknown levels
            return LogLevel.INFO;
        }
    }
}
