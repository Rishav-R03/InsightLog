package com.logaggregator.alert;

@FunctionalInterface
public interface AlertListener {
    void onAlert(AlertEvent event);
}