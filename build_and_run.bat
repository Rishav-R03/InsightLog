@echo off
echo Building Log Aggregator...
call mvn clean compile

echo.
echo Running Log Aggregator...
call mvn exec:java

pause