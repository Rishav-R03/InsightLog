package com.logaggregator.processor;

import com.logaggregator.core.LogBuffer;
import com.logaggregator.core.LogEntry;
import com.logaggregator.storage.LogStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class LogProcessor {
    private static final Logger logger = LoggerFactory.getLogger(LogProcessor.class);

    private final LogBuffer logBuffer;
    private final LogStorage logStorage;
    private final ExecutorService processorPool;
    private final AtomicLong processedCount;
    private volatile boolean running;

    public LogProcessor(LogBuffer logBuffer, LogStorage logStorage, int threadCount) {
        this.logBuffer = logBuffer;
        this.logStorage = logStorage;
        this.processorPool = Executors.newFixedThreadPool(threadCount);
        this.processedCount = new AtomicLong(0);
        this.running = false;
    }

    public void start() {
        running = true;
        int processorThreads = Runtime.getRuntime().availableProcessors();

        for (int i = 0; i < processorThreads; i++) {
            processorPool.submit(this::processLogs);
        }

        logger.info("Log processor started with {} threads", processorThreads);
    }

    public void stop() {
        running = false;
        processorPool.shutdown();
        logger.info("Log processor stopped. Total processed: {}", processedCount.get());
    }

    private void processLogs() {
        while (running) {
            try {
                List<LogEntry> batch = logBuffer.takeBatch();
                if (!batch.isEmpty()) {
                    processBatch(batch);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error processing log batch", e);
            }
        }
    }

    private void processBatch(List<LogEntry> batch) {
        try {
            // Store logs
            logStorage.storeBatch(batch);

            // Update metrics
            long count = processedCount.addAndGet(batch.size());

            if (count % 100 == 0) {
                logger.debug("Processed {} log entries total", count);
            }

            // Log batch statistics
            if (logger.isDebugEnabled()) {
                batch.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                LogEntry::getLevel,
                                java.util.stream.Collectors.counting()
                        ))
                        .forEach((level, cnt) -> {
                            if (cnt > 0) {
                                logger.debug("Batch stats - {}: {}", level, cnt);
                            }
                        });
            }

        } catch (Exception e) {
            logger.error("Failed to process batch of {} entries", batch.size(), e);
        }
    }

    public long getProcessedCount() {
        return processedCount.get();
    }

    public boolean isRunning() {
        return running;
    }
}