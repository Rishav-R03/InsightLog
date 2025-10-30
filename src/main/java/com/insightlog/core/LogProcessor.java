package com.insightlog.core;

import com.insightlog.model.LogEntryModel;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogProcessor implements Runnable{
    private static final Logger LOGGER = LoggerFactory.getLogger(LogProcessor.class);

    // Queue to consume raw log lines from the Collector
    private final BlockingQueue<String> rawLogQueue;

    private volatile boolean running = true;

    public LogProcessor(BlockingQueue<String> rawLogQueue) {
        this.rawLogQueue = rawLogQueue;
    }
    @Override
    public void run() {
        LOGGER.info("Log Processor started.");

        while (running) {
            try {
                // Blocks until a log line is available
                String rawLogLine = rawLogQueue.take();

                // --- WEEK 3 IMPLEMENTATION GOES HERE ---
                // 1. Concurrent parsing of the rawLogLine.
                // 2. Filtering and transformation.
                // 3. Storage (PostgreSQL & Inverted Index).

                LogEntryModel entry = processLine(rawLogLine);

                // Placeholder for actual database/index insertion
                LOGGER.debug("Processed and ready to store: {}", entry);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
                LOGGER.warn("Log Processor interrupted.");
            }
        }
        LOGGER.info("Log Processor stopped.");
    }

    public void stop() {
        this.running = false;
    }

    /**
     * Converts a raw log line into a structured LogEntry.
     * This method will become complex when handling multiple formats (JSON, syslog, etc.).
     */
    private LogEntryModel processLine(String rawLogLine) {
        // Minimal/Placeholder parsing for now:
        // Assume log format is simple: [LEVEL] MESSAGE
        String level = "INFO";
        String message = rawLogLine.trim();

        if (message.startsWith("[ERROR]")) {
            level = "ERROR";
            message = message.substring(7).trim();
        } else if (message.startsWith("[WARN]")) {
            level = "WARN";
            message = message.substring(6).trim();
        } else if (message.startsWith("[INFO]")) {
            level = "INFO";
            message = message.substring(6).trim();
        }

        // Dummy service and file for initial run
        return new LogEntryModel(Instant.now(), "dummy-service", level, message, "/var/log/app.log");
    }

}
