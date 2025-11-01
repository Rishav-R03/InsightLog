package com.logaggregator.parser;

import com.logaggregator.core.LogEntry;
import com.logaggregator.core.LogLevel;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class TextLogParser implements LogParser{
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2})?\\s*\\[?(\\w+)?\\]?\\s*(.*)$",
            Pattern.DOTALL
    );

    @Override
    public boolean supports(String logFormat) {
        return "text".equalsIgnoreCase(logFormat) || "plain".equalsIgnoreCase(logFormat);
    }

    @Override
    public Optional<LogEntry> parse(String source, String rawLine) {
        if (rawLine == null || rawLine.trim().isEmpty()) {
            return Optional.empty();
        }

        Matcher matcher = LOG_PATTERN.matcher(rawLine.trim());

        LocalDateTime timestamp = LocalDateTime.now();
        LogLevel level = LogLevel.INFO;
        String message = rawLine;

        if (matcher.matches()) {
            // Try to extract timestamp and level from structured logs
            String timestampStr = matcher.group(1);
            String levelStr = matcher.group(2);
            String content = matcher.group(3);

            if (content != null && !content.isEmpty()) {
                message = content;
            }

            // Parse level if found
            if (levelStr != null) {
                level = parseLogLevelFromText(levelStr);
            }
        }

        // Detect common log patterns
        level = detectLogLevel(message, level);

        LogEntry entry = new LogEntry(source, rawLine, level, timestamp, message);
        entry.addField("raw_length", rawLine.length());

        return Optional.of(entry);
    }

    private LogLevel parseLogLevelFromText(String text) {
        String upperText = text.toUpperCase();
        if (upperText.contains("ERROR") || upperText.contains("ERR")) {
            return LogLevel.ERROR;
        } else if (upperText.contains("WARN") || upperText.contains("WARNING")) {
            return LogLevel.WARN;
        } else if (upperText.contains("DEBUG")) {
            return LogLevel.DEBUG;
        } else if (upperText.contains("TRACE")) {
            return LogLevel.TRACE;
        } else if (upperText.contains("FATAL")) {
            return LogLevel.FATAL;
        } else {
            return LogLevel.INFO;
        }
    }

    private LogLevel detectLogLevel(String message, LogLevel defaultLevel) {
        String upperMessage = message.toUpperCase();
        if (upperMessage.contains("EXCEPTION") || upperMessage.contains("ERROR") ||
                upperMessage.contains("FAILED") || upperMessage.contains("CRITICAL")) {
            return LogLevel.ERROR;
        } else if (upperMessage.contains("WARN") || upperMessage.contains("CAUTION")) {
            return LogLevel.WARN;
        } else if (upperMessage.contains("DEBUG")) {
            return LogLevel.DEBUG;
        }
        return defaultLevel;
    }
}
