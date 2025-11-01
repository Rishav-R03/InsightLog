package com.logaggregator.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logaggregator.alert.AlertEvent;
import com.logaggregator.alert.AlertListener;
import com.logaggregator.core.LogEntry;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class LogWebSocket implements AlertListener {
    private static final Logger logger = LoggerFactory.getLogger(LogWebSocket.class);
    private static final Map<Session, LogWebSocket> clients = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    private Session session;

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        clients.put(session, this);
        logger.info("WebSocket client connected: {}", session.getRemoteAddress());

        // Send welcome message
        sendMessage(Map.of(
                "type", "connected",
                "message", "Connected to Log Aggregator",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        clients.remove(session);
        logger.info("WebSocket client disconnected: {}", reason);
    }

    @OnWebSocketError
    public void onError(Throwable error) {
        logger.error("WebSocket error", error);
        clients.remove(session);
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        logger.debug("Received WebSocket message: {}", message);
        // Handle client messages if needed
    }

    private void sendMessage(Object message) {
        if (session != null && session.isOpen()) {
            try {
                String json = mapper.writeValueAsString(message);
                session.getRemote().sendString(json);
            } catch (IOException e) {
                logger.error("Error sending WebSocket message", e);
            }
        }
    }

    @Override
    public void onAlert(AlertEvent event) {
        Map<String, Object> alertMessage = Map.of(
                "type", "alert",
                "alert", Map.of(
                        "rule", event.getRule().getName(),
                        "severity", event.getRule().getSeverity().name(),
                        "message", event.getMessage(),
                        "timestamp", event.getTimestamp().toString(),
                        "logEntry", Map.of(
                                "source", event.getTriggeringEntry().getSource(),
                                "level", event.getTriggeringEntry().getLevel().name(),
                                "message", event.getTriggeringEntry().getMessage()
                        )
                )
        );

        sendMessage(alertMessage);

        // Broadcast to all clients
        broadcast(alertMessage);
    }

    public void sendLogEntry(LogEntry entry) {
        Map<String, Object> logMessage = Map.of(
                "type", "log",
                "log", Map.of(
                        "id", entry.getId(),
                        "timestamp", entry.getTimestamp().toString(),
                        "source", entry.getSource(),
                        "level", entry.getLevel().name(),
                        "message", entry.getMessage(),
                        "fields", entry.getFields()
                )
        );

        sendMessage(logMessage);
    }

    public void sendStats(Map<String, Object> stats) {
        Map<String, Object> statsMessage = Map.of(
                "type", "stats",
                "stats", stats,
                "timestamp", java.time.LocalDateTime.now().toString()
        );

        sendMessage(statsMessage);
    }

    private static void broadcast(Object message) {
        clients.values().forEach(client -> {
            try {
                String json = mapper.writeValueAsString(message);
                client.session.getRemote().sendString(json);
            } catch (IOException e) {
                logger.error("Error broadcasting message", e);
            }
        });
    }

    public static void broadcastLogEntry(LogEntry entry) {
        Map<String, Object> logMessage = Map.of(
                "type", "log",
                "log", Map.of(
                        "id", entry.getId(),
                        "timestamp", entry.getTimestamp().toString(),
                        "source", entry.getSource(),
                        "level", entry.getLevel().name(),
                        "message", entry.getMessage()
                )
        );

        broadcast(logMessage);
    }

    public static void broadcastStats(Map<String, Object> stats) {
        Map<String, Object> statsMessage = Map.of(
                "type", "stats",
                "stats", stats,
                "timestamp", java.time.LocalDateTime.now().toString()
        );

        broadcast(statsMessage);
    }

    public static int getConnectedClients() {
        return clients.size();
    }
}