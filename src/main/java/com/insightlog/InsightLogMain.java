package com.insightlog;

import com.insightlog.core.LogCollector;
import com.insightlog.core.LogProcessor;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class to initialize and start the Log Aggregator System.
 */
public class InsightLogMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(InsightLogMain.class);

    // The shared queue for raw log lines (Producer-Consumer)
    private static final int QUEUE_CAPACITY = 1000;

    public static void main(String[] args) {
        LOGGER.info("Starting Log Aggregation System...");

        try {
            // 1. Initialize shared queue
            BlockingQueue<String> rawLogQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

            // 2. Define the log files/directories to watch
            // NOTE: Replace these with actual paths on your system for testing
            List<Path> logFiles = List.of(
                    Path.of("logs/app1.log"),
                    Path.of("logs/app2.log")
            // Add more paths as needed
            );

            // Ensure the logs directory exists (for easy testing)
            Path logDirectory = Path.of("logs");
            if (!logDirectory.toFile().exists()) {
                logDirectory.toFile().mkdirs();
                LOGGER.info("Created logs directory: {}", logDirectory.toAbsolutePath());
                // Create dummy files for watching
                for (Path file : logFiles) {
                    if (!file.toFile().exists()) {
                        file.toFile().createNewFile();
                        LOGGER.info("Created dummy log file: {}", file.getFileName());
                    }
                }
            }

            // 3. Initialize components
            LogCollector collector = new LogCollector(rawLogQueue, logFiles);
            LogProcessor processor = new LogProcessor(rawLogQueue);

            // 4. Start components using an ExecutorService (Thread Pool)
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(collector);
            // We can run multiple processors for true concurrency
            for (int i = 0; i < 4; i++) { // Start 4 consumer threads
                executor.submit(processor);
            }

            // 5. Shutdown hook for graceful termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutdown signal received. Initiating graceful shutdown...");
                collector.stop();
                processor.stop();
                executor.shutdown();
                LOGGER.info("System shut down successfully.");
            }));

            LOGGER.info("System initialization complete. Collector and Processors are running.");

            // Keep the main thread alive until shutdown
            Thread.currentThread().join();

        } catch (Exception e) {
            LOGGER.error("Fatal error during system startup: {}", e.getMessage(), e);
        }
    }
}
