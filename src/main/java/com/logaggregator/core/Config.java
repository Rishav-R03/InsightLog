package com.logaggregator.core;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = Config.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            }
            // Set defaults
            setDefaults();
        } catch (IOException e) {
            setDefaults();
        }
    }

    private static void setDefaults() {
        properties.setProperty("log.watch.directory", "logs");
        properties.setProperty("log.file.pattern", "*.log");
        properties.setProperty("log.buffer.size", "1000");
        properties.setProperty("log.batch.size", "100");
        properties.setProperty("log.batch.timeout.ms", "5000");
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public static long getLong(String key) {
        return Long.parseLong(properties.getProperty(key));
    }
}