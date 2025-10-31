package com.logaggregator;

import com.logaggregator.core.Config;
import com.logaggregator.core.LogEntry;
import com.logaggregator.core.LogLevel;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class Phase1Test {

    @Test
    public void testConfigLoading() {
        assertNotNull(Config.get("log.watch.directory"));
        assertEquals("logs", Config.get("log.watch.directory"));
        assertEquals(1000, Config.getInt("log.buffer.size"));
    }

    @Test
    public void testLogEntryCreation() {
        LogEntry entry = new LogEntry(
                "test-app",
                "raw log line",
                LogLevel.INFO,
                LocalDateTime.now(),
                "Test message"
        );

        assertNotNull(entry.getId());
        assertEquals("test-app", entry.getSource());
        assertEquals(LogLevel.INFO, entry.getLevel());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    public void testLogLevels() {
        assertEquals(LogLevel.INFO, LogLevel.valueOf("INFO"));
        assertEquals(LogLevel.ERROR, LogLevel.valueOf("ERROR"));
    }
}