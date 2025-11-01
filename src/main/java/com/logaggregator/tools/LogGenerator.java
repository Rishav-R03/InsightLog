package com.logaggregator.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogGenerator {
    public static void main(String[] args) throws IOException, InterruptedException {
        String logFile = "logs/sample-app.log";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            for (int i = 1; i <= 10; i++) {
                String logEntry = String.format(
                        "{\"timestamp\": \"%s\", \"level\": \"%s\", \"message\": \"Test log entry %d\", \"source\": \"sample-app\", \"requestId\": \"req-%d\"}",
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        i % 5 == 0 ? "ERROR" : i % 3 == 0 ? "WARN" : "INFO",
                        i,
                        i
                );

                writer.write(logEntry);
                writer.newLine();
                writer.flush();

                System.out.println("Generated: " + logEntry);
                Thread.sleep(2000); // Write every 2 seconds
            }
        }

        System.out.println("Log generation completed");
    }
}
