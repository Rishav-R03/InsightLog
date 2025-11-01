package com.logaggregator.tools;

import com.logaggregator.core.LogEntry;
import com.logaggregator.storage.LogStorage;
import java.util.List;

public class SearchTester {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Search Tester - This will be used in Phase 4");
        System.out.println("For now, check the main application logs to see processing in action!");

        // Wait a bit to see some logs processed
        Thread.sleep(10000);
        System.out.println("Search tester completed.");
    }

    public static void performSearch(LogStorage storage, String query) {
        if (storage == null) {
            System.out.println("Storage not available yet");
            return;
        }

        List<LogEntry> results = storage.search(query, 10);
        System.out.println("Search results for '" + query + "': " + results.size() + " entries");

        results.forEach(entry -> {
            System.out.printf("[%s] %s %s: %s%n",
                    entry.getTimestamp().toLocalTime(),
                    entry.getLevel(),
                    entry.getSource(),
                    entry.getMessage());
        });
    }
}