package com.insightlog.core;

import com.insightlog.model.LogEntryModel;

import java.awt.image.LookupOp;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogCollector implements Runnable{
    private static final Logger LOGGER = LoggerFactory.getLogger(LogCollector.class);
    // Queue to hold raw log lines before processing
    private final BlockingQueue<String> rawLogQueue;

    // List of file path to watch
    private final List<Path> logFilesToWatch;

    private volatile boolean running = true;

    public LogCollector(BlockingQueue<String> rawLogQueue,List<Path> logFilesToWatch){
            this.logFilesToWatch = logFilesToWatch;
            this.rawLogQueue = rawLogQueue;
    }
    @Override
    public void run(){
        LOGGER.info("Log Collector started. Watching {} files.",logFilesToWatch);
        //1. Initialize WatchService and register logDirectories.
        // 2. Start initial reading of existing log files.
        // 3. Loop to wait for new events (CREATE, MODIFY) from WatchService.
        // 4. On MODIFY event, implement the "tail -f" logic (read from the last known position).
        // 5. Place raw log lines into rawLogQueue.

        while (running){
            try{
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
                LOGGER.warn("Log collector interrupted");
            }
        }
        LOGGER.info("Log Collector Stopped");
    }

    public void stop(){
        this.running = false;
    }
    public String parseRawLog(String rawLine){
        return rawLine;
    }
}
