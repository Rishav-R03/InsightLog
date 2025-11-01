package com.logaggregator;

import com.logaggregator.collector.LogFileWatcher;
import com.logaggregator.core.Config;
import com.logaggregator.core.LogBuffer;
import com.logaggregator.core.LogLevel;
import com.logaggregator.parser.ParserRegistry;
import com.logaggregator.processor.LogProcessor;
import com.logaggregator.storage.InMemoryStorage;
import com.logaggregator.storage.LogStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static volatile boolean running = true;

    private static LogFileWatcher fileWatcher;
    private static LogBuffer logBuffer;
    private static ParserRegistry parserRegistry;
    private static LogStorage logStorage;
    private static LogProcessor logProcessor;
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
        logger.info("  Processor Threads: {}", Config.get("log.processor.threads"));
        logger.info("  Storage Capacity: {}", Config.get("log.storage.max_entries"));
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

        // Phase 3: Storage and Processing
        logStorage = new InMemoryStorage(Config.getInt("log.storage.max_entries"));
        logProcessor = new LogProcessor(
                logBuffer,
                logStorage,
                Config.getInt("log.processor.threads")
        );

        scheduler = Executors.newScheduledThreadPool(3);

        logger.info("✓ Log buffer initialized (capacity: {})", Config.getInt("log.buffer.size"));
        logger.info("✓ Parser registry initialized ({} parsers)", parserRegistry.getParsers().size());
        logger.info("✓ File watcher initialized");
        logger.info("✓ In-memory storage initialized (capacity: {})", Config.getInt("log.storage.max_entries"));
        logger.info("✓ Log processor initialized ({} threads)", Config.getInt("log.processor.threads"));
        logger.info("✓ Scheduler initialized");
    }

    private static void startSystem() throws Exception {
        logger.info("Starting system components...");

        fileWatcher.start();
        logProcessor.start();

        // Start buffer processor monitoring
        scheduler.scheduleAtFixedRate(() -> {
            int bufferSize = logBuffer.size();
            long processedCount = logProcessor.getProcessedCount();
            long storageCount = logStorage.getTotalCount();

            if (bufferSize > 0 || processedCount % 100 == 0) {
                logger.info("Processing stats - Buffer: {}, Processed: {}, Storage: {}",
                        bufferSize, processedCount, storageCount);
            }
        }, 10, 10, TimeUnit.SECONDS);

        logger.info("✓ File watcher started");
        logger.info("✓ Log processor started");
        logger.info("✓ Monitoring tasks scheduled");
        logger.info("Phase 3: Processing & Storage is ACTIVE - Ready to process and store logs!");
    }

    private static void startMonitoring() {
        // Log system status periodically
        scheduler.scheduleAtFixedRate(() -> {
            long processedCount = logProcessor.getProcessedCount();
            long storageCount = logStorage.getTotalCount();
            int bufferSize = logBuffer.size();

            logger.info("System Status - Buffer: {}, Processed: {}, Storage: {}",
                    bufferSize, processedCount, storageCount);

            // Show storage statistics
            if (storageCount > 0) {
                Map<LogLevel, Long> stats =
                        ((InMemoryStorage) logStorage).getLevelStatistics();
                stats.forEach((level, count) -> {
                    if (count > 0) {
                        logger.info("  {}: {}", level, count);
                    }
                });
            }

        }, 30, 30, TimeUnit.SECONDS);
    }

    private static void shutdown() {
        logger.info("Shutting down Log Aggregator...");
        running = false;

        if (logProcessor != null) {
            logProcessor.stop();
        }

        if (fileWatcher != null) {
            fileWatcher.stop();
        }

        if (logStorage != null) {
            logStorage.close();
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

    // Utility method to access storage for testing
    public static LogStorage getLogStorage() {
        return logStorage;
    }
}