package com.logaggregator.web;

import com.logaggregator.core.LogEntry;
import com.logaggregator.storage.InMemoryStorage;
import com.logaggregator.storage.LogStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LogApiServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(LogApiServlet.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    // This would be injected in a real application
    private LogStorage getStorage() {
        return com.logaggregator.Main.getLogStorage();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null) pathInfo = "";

        try {
            switch (pathInfo) {
                case "/search":
                    handleSearch(req, resp);
                    break;
                case "/stats":
                    handleStats(req, resp);
                    break;
                case "/recent":
                    handleRecent(req, resp);
                    break;
                case "/health":
                    handleHealth(req, resp);
                    break;
                default:
                    handleDefault(req, resp);
                    break;
            }
        } catch (Exception e) {
            logger.error("API error", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    private void handleSearch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("q");
        String limitStr = req.getParameter("limit");
        int limit = limitStr != null ? Integer.parseInt(limitStr) : 50;

        if (query == null || query.trim().isEmpty()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Query parameter 'q' is required");
            return;
        }

        LogStorage storage = getStorage();
        if (storage == null) {
            sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Storage not available");
            return;
        }

        List<LogEntry> results = storage.search(query, limit);
        List<Map<String, Object>> formattedResults = results.stream()
                .map(this::formatLogEntry)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("query", query);
        response.put("results", formattedResults);
        response.put("total", formattedResults.size());
        response.put("timestamp", java.time.LocalDateTime.now().toString());

        mapper.writeValue(resp.getWriter(), response);
    }

    private void handleStats(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LogStorage storage = getStorage();
        Map<String, Object> stats = new HashMap<>();

        if (storage instanceof InMemoryStorage) {
            InMemoryStorage memStorage = (InMemoryStorage) storage;
            stats.put("totalEntries", storage.getTotalCount());
            stats.put("levelDistribution", memStorage.getLevelStatistics());
            stats.put("connectedClients", LogWebSocket.getConnectedClients());
        } else {
            stats.put("totalEntries", storage.getTotalCount());
        }

        stats.put("timestamp", java.time.LocalDateTime.now().toString());

        mapper.writeValue(resp.getWriter(), stats);
    }

    private void handleRecent(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String limitStr = req.getParameter("limit");
        int limit = limitStr != null ? Integer.parseInt(limitStr) : 20;

        LogStorage storage = getStorage();
        if (storage instanceof InMemoryStorage) {
            InMemoryStorage memStorage = (InMemoryStorage) storage;
            List<LogEntry> recent = memStorage.getRecentEntries(limit);
            List<Map<String, Object>> formattedResults = recent.stream()
                    .map(this::formatLogEntry)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("results", formattedResults);
            response.put("total", formattedResults.size());
            response.put("timestamp", java.time.LocalDateTime.now().toString());

            mapper.writeValue(resp.getWriter(), response);
        } else {
            sendError(resp, HttpServletResponse.SC_NOT_IMPLEMENTED, "Recent endpoint only available for in-memory storage");
        }
    }

    private void handleHealth(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("timestamp", java.time.LocalDateTime.now().toString());
        health.put("storage", getStorage() != null ? "available" : "unavailable");

        mapper.writeValue(resp.getWriter(), health);
    }

    private void handleDefault(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Log Aggregator API");
        info.put("version", "1.0.0");
        info.put("endpoints", List.of(
                "/api/search?q=query&limit=50",
                "/api/stats",
                "/api/recent?limit=20",
                "/api/health"
        ));

        mapper.writeValue(resp.getWriter(), info);
    }

    private Map<String, Object> formatLogEntry(LogEntry entry) {
        Map<String, Object> formatted = new HashMap<>();
        formatted.put("id", entry.getId());
        formatted.put("timestamp", entry.getTimestamp().toString());
        formatted.put("source", entry.getSource());
        formatted.put("level", entry.getLevel().name());
        formatted.put("message", entry.getMessage());
        formatted.put("fields", entry.getFields());
        return formatted;
    }

    private void sendError(HttpServletResponse resp, int code, String message) throws IOException {
        resp.setStatus(code);
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("code", code);
        mapper.writeValue(resp.getWriter(), error);
    }
}