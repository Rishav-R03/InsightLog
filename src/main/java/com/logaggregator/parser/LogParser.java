package com.logaggregator.parser;
import com.logaggregator.core.LogEntry;
import java.util.Optional;

public interface LogParser {
    boolean supports(String logFormat);
    Optional<LogEntry> parse(String source, String rawLine);
}