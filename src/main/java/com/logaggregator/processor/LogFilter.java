package com.logaggregator.processor;

import com.logaggregator.core.LogEntry;
import com.logaggregator.core.LogLevel;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class LogFilter {

    public static Predicate<LogEntry> createLevelFilter(List<LogLevel> includedLevels) {
        return entry -> includedLevels.contains(entry.getLevel());
    }

    public static Predicate<LogEntry> createSourceFilter(List<String> includedSources) {
        return entry -> includedSources.contains(entry.getSource());
    }

    public static Predicate<LogEntry> createMessageFilter(String regexPattern) {
        Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        return entry -> pattern.matcher(entry.getMessage()).find();
    }

    public static Predicate<LogEntry> createFieldFilter(String fieldName, String fieldValue) {
        return entry -> {
            Object value = entry.getFields().get(fieldName);
            return value != null && value.toString().equals(fieldValue);
        };
    }

    public static Predicate<LogEntry> combineFilters(List<Predicate<LogEntry>> filters) {
        return filters.stream()
                .reduce(Predicate::and)
                .orElse(entry -> true);
    }
}