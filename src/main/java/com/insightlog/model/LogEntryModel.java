package com.insightlog.model;

import java.time.Instant;
import java.util.UUID;

public class LogEntryModel {
    private final String id;
    private final Instant timestamp;
    private final String service;
    private final String level;
    private final String message;
    private final String sourceFile;

    public LogEntryModel(Instant timestamp,String service,String level, String message,String sourceFile){
        this.id = UUID.randomUUID().toString();
        this.timestamp = timestamp;
        this.service = service;
        this.level = level;
        this.message = message;
        this.sourceFile =sourceFile;
    }
    public String getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getService() {
        return service;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    @Override
    public String toString(){
        return String.format("[%s] [@s] %s: %s",timestamp,level,service,message);
    }
}
