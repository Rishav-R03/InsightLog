package com.logaggregator.parser;

import com.logaggregator.core.LogEntry;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ParserRegistry {
    private final List<LogParser> parsers;

    public ParserRegistry() {
        this.parsers = new CopyOnWriteArrayList<>();
        registerDefaultParsers();
    }

    private void registerDefaultParsers() {
        registerParser(new JsonLogParser());
        registerParser(new TextLogParser());
    }

    public void registerParser(LogParser parser) {
        parsers.add(parser);
    }

    public Optional<LogEntry> parseLine(String source, String line, String formatHint) {
        // First try format-specific parser
        for (LogParser parser : parsers) {
            if (parser.supports(formatHint)) {
                Optional<LogEntry> result = parser.parse(source, line);
                if (result.isPresent()) {
                    return result;
                }
            }
        }

        // Then try all parsers
        for (LogParser parser : parsers) {
            Optional<LogEntry> result = parser.parse(source, line);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    public List<LogParser> getParsers() {
        return new ArrayList<>(parsers);
    }

}
