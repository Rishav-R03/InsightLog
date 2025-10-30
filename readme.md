## Real-time Log Aggregation & Monitoring System

This project implements a lightweight, real-time log aggregation and monitoring system using Java 17+, designed as an alternative to full-fledged ELK stacks for smaller-scale deployments.

## Core Architecture

The system follows a classic Producer-Consumer pattern:

`Log Collector (Producer)`: Monitors specified log files in real time (using tail -f style watching) and places raw log lines onto a shared queue.

`Log Processor (Consumer)`: Reads from the queue, parses the raw lines into structured LogEntry objects, performs filtering/transformation, and concurrently stores them in the PostgreSQL database and the in-memory Inverted Index.

Project Phases

Phase

Duration

Core Features

Status

1

Initialization

Project setup, Maven, Core Data Model

Complete

2

Log Collection

File Watchers (WatchService), Real-time reading, Log Format Parsing

Pending

3

Processing & Storage

Concurrent Parsing, Time-Series PostgreSQL Storage, Inverted Index

Pending

4

Analytics & Alerts

Full-text Search, Metrics Aggregation, Web Dashboard & Alerts

Pending

Technical Stack

Core: Java 17+

Build Tool: Maven

Concurrency: java.util.concurrent (ExecutorService, BlockingQueue)

Storage: PostgreSQL

Web: Simple embedded HTTP server (TBD)
