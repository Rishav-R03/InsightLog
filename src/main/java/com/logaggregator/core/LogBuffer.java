package com.logaggregator.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.ArrayList;
import java.util.List;

public class LogBuffer {
    private final BlockingQueue<LogEntry> buffer;
    private final int batchSize;
    private final long batchTimeoutMs;

    public LogBuffer(int capacity, int batchSize, long batchTimeoutMs) {
        this.buffer = new LinkedBlockingQueue<>(capacity);
        this.batchSize = batchSize;
        this.batchTimeoutMs = batchTimeoutMs;
    }

    public boolean offer(LogEntry entry) {
        return buffer.offer(entry);
    }

    public void put(LogEntry entry) throws InterruptedException {
        buffer.put(entry);
    }

    public List<LogEntry> takeBatch() throws InterruptedException {
        List<LogEntry> batch = new ArrayList<>(batchSize);

        // Wait for first element
        LogEntry firstEntry = buffer.take();
        batch.add(firstEntry);

        // Try to gather more elements up to batchSize or timeout
        long endTime = System.currentTimeMillis() + batchTimeoutMs;

        while (batch.size() < batchSize && System.currentTimeMillis() < endTime) {
            LogEntry entry = buffer.poll();
            if (entry != null) {
                batch.add(entry);
            } else {
                Thread.sleep(10); // Small sleep to prevent busy waiting
            }
        }

        return batch;
    }

    public int size() {
        return buffer.size();
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    public int remainingCapacity() {
        return buffer.remainingCapacity();
    }
}
