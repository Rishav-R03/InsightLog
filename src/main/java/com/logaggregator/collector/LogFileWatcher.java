package com.logaggregator.collector;

import com.logaggregator.core.LogBuffer;
import com.logaggregator.parser.ParserRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardWatchEventKinds.*;

public class LogFileWatcher {
    private static final Logger logger = LoggerFactory.getLogger(LogFileWatcher.class);

    private final Path watchDirectory;
    private final String filePattern;
    private final LogBuffer buffer;
    private final ParserRegistry parserRegistry;
    private final AtomicBoolean running;
    private final ExecutorService executor;

    public LogFileWatcher(String watchDir, String filePattern,
                          LogBuffer buffer, ParserRegistry parserRegistry) {
        this.watchDirectory = Paths.get(watchDir);
        this.filePattern = filePattern;
        this.buffer = buffer;
        this.parserRegistry = parserRegistry;
        this.running = new AtomicBoolean(false);
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        if (!Files.exists(watchDirectory)) {
            Files.createDirectories(watchDirectory);
            logger.info("Created watch directory: {}", watchDirectory.toAbsolutePath());
        }

        running.set(true);

        // Start watching for new files
        executor.submit(this::watchForNewFiles);

        // Start watching existing files
        watchExistingFiles();

        logger.info("Log file watcher started for directory: {}", watchDirectory);
    }

    public void stop() {
        running.set(false);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Log file watcher stopped");
    }

    private void watchForNewFiles() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            watchDirectory.register(watchService, ENTRY_CREATE, ENTRY_MODIFY);

            while (running.get()) {
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                if (key == null) continue;

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == OVERFLOW) continue;

                    Path fileName = (Path) event.context();
                    if (matchesFilePattern(fileName.toString())) {
                        Path filePath = watchDirectory.resolve(fileName);
                        if (Files.isRegularFile(filePath)) {
                            startTailingFile(filePath);
                        }
                    }
                }

                if (!key.reset()) {
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            if (running.get()) {
                logger.error("Error in file watcher", e);
            }
        }
    }

    private void watchExistingFiles() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(watchDirectory, fileName -> matchesFilePattern(String.valueOf(fileName)))) {
            for (Path filePath : stream) {
                if (Files.isRegularFile(filePath)) {
                    startTailingFile(filePath);
                }
            }
        }
    }

    private void startTailingFile(Path filePath) {
        executor.submit(() -> {
            try {
                tailFile(filePath);
            } catch (IOException e) {
                logger.error("Error tailing file: {}", filePath, e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void tailFile(Path filePath) throws IOException, InterruptedException {
        logger.info("Started tailing file: {}", filePath);

        long lastPosition = 0;
        String fileName = filePath.getFileName().toString();
        String formatHint = detectFormatHint(fileName);

        while (running.get() && Files.exists(filePath)) {
            try {
                long fileSize = Files.size(filePath);

                if (fileSize < lastPosition) {
                    // File was truncated/rotated
                    lastPosition = 0;
                }

                if (fileSize > lastPosition) {
                    try (var fileChannel = FileChannel.open(filePath, StandardOpenOption.READ)) {
                        var buffer = java.nio.ByteBuffer.allocate(8192);
                        fileChannel.position(lastPosition);

                        int bytesRead;
                        while ((bytesRead = fileChannel.read(buffer)) > 0) {
                            buffer.flip();
                            String newContent = new String(buffer.array(), 0, bytesRead);
                            processNewContent(fileName, newContent, formatHint);
                            buffer.clear();
                        }

                        lastPosition = fileChannel.position();
                    }
                }

                Thread.sleep(100); // Check for new content every 100ms

            } catch (IOException e) {
                logger.warn("Error reading file {}, retrying...", filePath, e);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.info("Stopped tailing file: {}", filePath);
    }

    private void processNewContent(String source, String content, String formatHint) {
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                parserRegistry.parseLine(source, line, formatHint)
                        .ifPresent(entry -> {
                            if (!buffer.offer(entry)) {
                                logger.warn("Buffer full, dropping log entry: {}", entry.getMessage());
                            }
                        });
            }
        }
    }

    private boolean matchesFilePattern(String fileName) {
        return fileName.matches(filePattern.replace("*", ".*").replace("?", "."));
    }

    private String detectFormatHint(String fileName) {
        if (fileName.endsWith(".json")) {
            return "json";
        }
        return "text";
    }
}
