package com.logaggregator;

import com.logaggregator.collector.LogFileWatcher;
import com.logaggregator.core.Config;
import com.logaggregator.core.LogBuffer;
import com.logaggregator.parser.ParserRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static volatile boolean running = true;

    private static LogFileWatcher fileWatcher;
    private static LogBuffer logBuffer;
    private static ParserRegistry parserRegistry;
    private static ScheduledExecutorService scheduler;

    public static void main(String[] args) {
        logger.info("Starting Log Aggregator System");

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received");
            running = false;
            shutdown();
        }));

        try {
            // Print configuration
            printConfiguration();

            // Initialize and start components
            initializeSystem();
            startSystem();

            // Start monitoring thread
            startMonitoring();

            // Keep the main thread alive
            logger.info("Log Aggregator is running. Press Ctrl+C to stop.");
            while (running) {
                Thread.sleep(1000);
            }

        } catch (InterruptedException e) {
            logger.info("Log Aggregator interrupted");
        } catch (Exception e) {
            logger.error("Failed to start Log Aggregator", e);
        } finally {
            shutdown();
        }
    }

    private static void printConfiguration() {
        logger.info("Configuration:");
        logger.info("  Watch Directory: {}", Config.get("log.watch.directory"));
        logger.info("  File Pattern: {}", Config.get("log.file.pattern"));
        logger.info("  Buffer Size: {}", Config.get("log.buffer.size"));
        logger.info("  Batch Size: {}", Config.get("log.batch.size"));
        logger.info("  Batch Timeout: {}ms", Config.get("log.batch.timeout.ms"));
    }

    private static void initializeSystem() {
        logger.info("Initializing system components...");

        // Initialize core components
        logBuffer = new LogBuffer(
                Config.getInt("log.buffer.size"),
                Config.getInt("log.batch.size"),
                Config.getLong("log.batch.timeout.ms")
        );

        parserRegistry = new ParserRegistry();

        fileWatcher = new LogFileWatcher(
                Config.get("log.watch.directory"),
                Config.get("log.file.pattern"),
                logBuffer,
                parserRegistry
        );

        scheduler = Executors.newScheduledThreadPool(2);

        logger.info("✓ Log buffer initialized (capacity: {})", Config.getInt("log.buffer.size"));
        logger.info("✓ Parser registry initialized ({} parsers)", parserRegistry.getParsers().size());
        logger.info("✓ File watcher initialized");
        logger.info("✓ Scheduler initialized");
    }

    private static void startSystem() throws Exception {
        logger.info("Starting system components...");

        fileWatcher.start();

        // Start buffer processor (for Phase 3 - will process batches)
        scheduler.scheduleAtFixedRate(() -> {
            int bufferSize = logBuffer.size();
            if (bufferSize > 0) {
                logger.debug("Buffer status: {} entries waiting", bufferSize);
            }
        }, 5, 5, TimeUnit.SECONDS);

        logger.info("✓ File watcher started");
        logger.info("✓ Monitoring tasks scheduled");
        logger.info("Phase 2: Log Collection is ACTIVE - Watching for log files...");
    }

    private static void startMonitoring() {
        // Log system status periodically
        scheduler.scheduleAtFixedRate(() -> {
            logger.info("System status - Buffer: {} entries, Active parsers: {}",
                    logBuffer.size(), parserRegistry.getParsers().size());
        }, 30, 30, TimeUnit.SECONDS);
    }

    private static void shutdown() {
        logger.info("Shutting down Log Aggregator...");
        running = false;

        if (fileWatcher != null) {
            fileWatcher.stop();
        }

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }

        logger.info("Log Aggregator shutdown completed");
    }
}