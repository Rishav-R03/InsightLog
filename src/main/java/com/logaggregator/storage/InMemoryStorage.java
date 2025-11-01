package com.logaggregator.storage;

import com.logaggregator.core.LogEntry;
import com.logaggregator.core.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class InMemoryStorage implements LogStorage {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryStorage.class);

    private final List<LogEntry> logEntries;
    private final Map<String, List<LogEntry>> invertedIndex;
    private final ReadWriteLock lock;
    private final long maxCapacity;
    private long totalCount;

    public InMemoryStorage(long maxCapacity) {
        this.logEntries = new CopyOnWriteArrayList<>();
        this.invertedIndex = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.maxCapacity = maxCapacity;
        this.totalCount = 0;
    }

    @Override
    public void store(LogEntry entry) {
        storeBatch(Collections.singletonList(entry));
    }

    @Override
    public void storeBatch(List<LogEntry> entries) {
        if (entries.isEmpty()) return;

        lock.writeLock().lock();
        try {
            // Check capacity and evict if necessary
            while (logEntries.size() + entries.size() > maxCapacity && !logEntries.isEmpty()) {
                LogEntry removed = logEntries.remove(0);
                removeFromIndex(removed);
            }

            // Add to storage
            logEntries.addAll(entries);

            // Add to inverted index
            for (LogEntry entry : entries) {
                addToIndex(entry);
            }

            totalCount += entries.size();

            if (logger.isDebugEnabled()) {
                logger.debug("Stored {} entries, total: {}, storage size: {}",
                        entries.size(), totalCount, logEntries.size());
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<LogEntry> search(String query, int limit) {
        lock.readLock().lock();
        try {
            if (query == null || query.trim().isEmpty()) {
                // Return recent entries
                return logEntries.stream()
                        .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                        .limit(limit)
                        .collect(Collectors.toList());
            }

            // Simple keyword search using inverted index
            String[] keywords = query.toLowerCase().split("\\s+");
            Map<LogEntry, Integer> scoreMap = new HashMap<>();

            for (String keyword : keywords) {
                List<LogEntry> matches = invertedIndex.getOrDefault(keyword, Collections.emptyList());
                for (LogEntry entry : matches) {
                    scoreMap.put(entry, scoreMap.getOrDefault(entry, 0) + 1);
                }
            }

            // Sort by score and timestamp
            return scoreMap.entrySet().stream()
                    .sorted((a, b) -> {
                        int scoreCompare = b.getValue().compareTo(a.getValue());
                        if (scoreCompare != 0) return scoreCompare;
                        return b.getKey().getTimestamp().compareTo(a.getKey().getTimestamp());
                    })
                    .map(Map.Entry::getKey)
                    .limit(limit)
                    .collect(Collectors.toList());

        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public long getTotalCount() {
        return totalCount;
    }

    @Override
    public void close() {
        logger.info("In-memory storage closed. Total entries processed: {}", totalCount);
    }

    private void addToIndex(LogEntry entry) {
        // Index message
        indexText(entry.getMessage(), entry);

        // Index source
        indexText(entry.getSource(), entry);

        // Index level
        indexText(entry.getLevel().name(), entry);

        // Index additional fields
        entry.getFields().forEach((key, value) -> {
            if (value != null) {
                indexText(value.toString(), entry);
            }
        });
    }

    private void indexText(String text, LogEntry entry) {
        if (text == null) return;

        String[] words = text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .split("\\s+");

        for (String word : words) {
            if (word.length() > 2) { // Ignore very short words
                invertedIndex.computeIfAbsent(word, k -> new ArrayList<>())
                        .add(entry);
            }
        }
    }

    private void removeFromIndex(LogEntry entry) {
        // This is a simplified implementation
        // In a real system, we'd track which words were indexed for each entry
        invertedIndex.values().forEach(list -> list.remove(entry));
        invertedIndex.entrySet().removeIf(entryList -> entryList.getValue().isEmpty());
    }

    // Additional utility methods
    public List<LogEntry> getRecentEntries(int count) {
        lock.readLock().lock();
        try {
            return logEntries.stream()
                    .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                    .limit(count)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<LogLevel, Long> getLevelStatistics() {
        lock.readLock().lock();
        try {
            return logEntries.stream()
                    .collect(Collectors.groupingBy(
                            LogEntry::getLevel,
                            Collectors.counting()
                    ));
        } finally {
            lock.readLock().unlock();
        }
    }
}