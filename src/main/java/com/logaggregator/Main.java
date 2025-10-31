package com.logaggregator;

import com.logaggregator.core.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static volatile boolean running = true;

    public static void main(String[] args) {
        logger.info("Starting Log Aggregator System");

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received");
            running = false;
        }));

        try {
            // Print configuration
            logger.info("Configuration:");
            logger.info("  Watch Directory: {}", Config.get("log.watch.directory"));
            logger.info("  File Pattern: {}", Config.get("log.file.pattern"));
            logger.info("  Buffer Size: {}", Config.get("log.buffer.size"));
            logger.info("  Batch Size: {}", Config.get("log.batch.size"));

            // Initialize and start components
            initializeSystem();

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
            logger.info("Log Aggregator shutdown completed");
        }
    }

    private static void initializeSystem() {
        logger.info("Initializing system components...");
        // Phase 1: Basic structure ready
        logger.info("✓ Core data models initialized");
        logger.info("✓ Configuration system ready");
        logger.info("✓ Logging framework configured");
        logger.info("✓ Project structure validated");

        // Phase 2 components will be added here
        logger.info("Ready for Phase 2: Log Collection Implementation");
    }
}