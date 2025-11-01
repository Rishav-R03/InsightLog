package com.logaggregator.storage;

import com.logaggregator.core.LogEntry;
import java.util.List;

public interface LogStorage {
    void store(LogEntry entry);
    void storeBatch(List<LogEntry> entries);
    List<LogEntry> search(String query, int limit);
    long getTotalCount();
    void close();
}