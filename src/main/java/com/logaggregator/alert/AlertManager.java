package com.logaggregator.alert;

import com.logaggregator.core.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlertManager {
    private static final Logger logger = LoggerFactory.getLogger(AlertManager.class);

    private final List<AlertRule> alertRules;
    private final List<AlertListener> listeners;
    private final ScheduledExecutorService scheduler;

    public AlertManager() {
        this.alertRules = new CopyOnWriteArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        loadDefaultRules();
        startAlertCleanup();
    }

    private void loadDefaultRules() {
        // Add some default alert rules
        addRule(new AlertRule(
                "error-detector",
                "Error Log Detector",
                ".*(error|exception|failed|critical|fatal).*",
                "message",
                AlertSeverity.HIGH,
                "Error pattern detected in logs"
        ));

        addRule(new AlertRule(
                "high-frequency",
                "High Frequency Logs",
                ".*",
                "message",
                AlertSeverity.MEDIUM,
                "High log frequency detected"
        ));

        logger.info("Loaded {} default alert rules", alertRules.size());
    }

    public void addRule(AlertRule rule) {
        alertRules.add(rule);
        logger.info("Added alert rule: {}", rule.getName());
    }

    public void removeRule(String ruleId) {
        alertRules.removeIf(rule -> rule.getId().equals(ruleId));
    }

    public List<AlertRule> getRules() {
        return new ArrayList<>(alertRules);
    }

    public void processLogEntry(LogEntry entry) {
        for (AlertRule rule : alertRules) {
            if (rule.matches(entry)) {
                triggerAlert(rule, entry);
            }
        }
    }

    private void triggerAlert(AlertRule rule, LogEntry triggeringEntry) {
        rule.trigger(triggeringEntry);

        AlertEvent event = new AlertEvent(
                rule,
                triggeringEntry,
                LocalDateTime.now(),
                rule.getMessage() + " - " + triggeringEntry.getMessage()
        );

        // Notify listeners
        for (AlertListener listener : listeners) {
            try {
                listener.onAlert(event);
            } catch (Exception e) {
                logger.error("Error notifying alert listener", e);
            }
        }

        logger.warn("ALERT TRIGGERED: {} - {}", rule.getName(), event.getMessage());
    }

    public void addListener(AlertListener listener) {
        listeners.add(listener);
    }

    public void removeListener(AlertListener listener) {
        listeners.remove(listener);
    }

    private void startAlertCleanup() {
        // Clean up old alerts periodically (in a real system)
        scheduler.scheduleAtFixedRate(() -> {
            // Could implement alert expiration here
        }, 1, 1, TimeUnit.HOURS);
    }

    public void stop() {
        scheduler.shutdown();
        logger.info("Alert manager stopped");
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRules", alertRules.size());
        stats.put("totalTriggers", alertRules.stream().mapToLong(AlertRule::getTriggerCount).sum());

        Map<AlertSeverity, Long> severityCounts = new HashMap<>();
        alertRules.forEach(rule -> {
            severityCounts.merge(rule.getSeverity(), rule.getTriggerCount(), Long::sum);
        });
        stats.put("triggersBySeverity", severityCounts);

        return stats;
    }
}