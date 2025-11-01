@echo off
echo Testing Log Aggregator Application...
echo.

echo 1. Starting application...
start cmd /k "mvn exec:java"

echo 2. Waiting for application to start...
timeout /t 5

echo 3. Generating test logs...
start cmd /k "mvn exec:java -Dexec.mainClass=\"com.logaggregator.tools.LogGenerator\""

echo 4. Opening dashboard...
start http://localhost:8000

echo.
echo Test setup complete!
echo - Application is running in first window
echo - Log generator is running in second window
echo - Dashboard should open in your browser
echo.
echo Press any key to stop all processes...
pause

taskkill /f /im java.exe 2>nul
echo All processes stopped.