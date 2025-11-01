package com.logaggregator.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        out.write("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Log Aggregator Dashboard</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { 
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background: #0f0f23; color: #cccccc;
                        line-height: 1.6;
                    }
                    .container { 
                        max-width: 1200px; 
                        margin: 0 auto; 
                        padding: 20px;
                    }
                    .header { 
                        background: #1a1a2e; 
                        padding: 20px; 
                        border-radius: 10px;
                        margin-bottom: 20px;
                        border: 1px solid #333344;
                    }
                    .stats-grid { 
                        display: grid; 
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); 
                        gap: 15px; 
                        margin-bottom: 20px;
                    }
                    .stat-card { 
                        background: #16213e; 
                        padding: 15px; 
                        border-radius: 8px;
                        border-left: 4px solid #0ea5e9;
                    }
                    .panels { 
                        display: grid; 
                        grid-template-columns: 1fr 1fr; 
                        gap: 20px; 
                    }
                    .panel { 
                        background: #1a1a2e; 
                        padding: 20px; 
                        border-radius: 10px;
                        border: 1px solid #333344;
                    }
                    .log-entry { 
                        background: #16213e; 
                        margin: 10px 0; 
                        padding: 12px; 
                        border-radius: 6px;
                        border-left: 4px solid #0ea5e9;
                    }
                    .log-entry.error { border-left-color: #ef4444; }
                    .log-entry.warn { border-left-color: #f59e0b; }
                    .log-entry.info { border-left-color: #0ea5e9; }
                    
                    .search-box {
                        background: #16213e;
                        padding: 15px;
                        border-radius: 8px;
                        margin-bottom: 20px;
                    }
                    
                    input, button {
                        padding: 10px;
                        margin: 5px;
                        border: 1px solid #333344;
                        border-radius: 5px;
                        background: #0f0f23;
                        color: #cccccc;
                    }
                    
                    button {
                        background: #0ea5e9;
                        color: white;
                        cursor: pointer;
                    }
                    
                    button:hover {
                        background: #0284c7;
                    }
                    
                    .connection-status {
                        float: right;
                        padding: 5px 10px;
                        border-radius: 15px;
                        font-size: 12px;
                        background: #10b981; 
                        color: white;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📊 Log Aggregator Dashboard</h1>
                        <div class="connection-status">Connected</div>
                        <p>Real-time log monitoring and analysis</p>
                    </div>

                    <div class="stats-grid" id="statsGrid">
                        <div class="stat-card">
                            <h3>Total Logs</h3>
                            <div id="totalLogs">Loading...</div>
                        </div>
                        <div class="stat-card">
                            <h3>INFO Logs</h3>
                            <div id="infoLogs">Loading...</div>
                        </div>
                        <div class="stat-card">
                            <h3>WARN Logs</h3>
                            <div id="warnLogs">Loading...</div>
                        </div>
                        <div class="stat-card">
                            <h3>ERROR Logs</h3>
                            <div id="errorLogs">Loading...</div>
                        </div>
                    </div>

                    <div class="search-box">
                        <input type="text" id="searchInput" placeholder="Search logs..." style="width: 300px;">
                        <button onclick="performSearch()">Search</button>
                        <button onclick="loadRecentLogs()">Refresh Logs</button>
                        <button onclick="loadStats()">Refresh Stats</button>
                        <button onclick="clearLogs()">Clear Display</button>
                    </div>

                    <div class="panels">
                        <div class="panel">
                            <h2>📝 Logs</h2>
                            <div id="recentLogs">
                                <p>Loading logs...</p>
                            </div>
                        </div>
                        
                        <div class="panel">
                            <h2>📊 Statistics</h2>
                            <div id="statsPanel">
                                <p>Loading statistics...</p>
                            </div>
                            <div style="margin-top: 20px;">
                                <h3>Quick Actions:</h3>
                                <button onclick="testAPI()">Test API</button>
                                <button onclick="viewAllEndpoints()">View Endpoints</button>
                            </div>
                        </div>
                    </div>
                </div>

                <script>
                    function loadStats() {
                        fetch('/api/stats')
                            .then(response => {
                                if (!response.ok) throw new Error('API error: ' + response.status);
                                return response.json();
                            })
                            .then(data => {
                                console.log('Stats loaded:', data);
                                document.getElementById('totalLogs').textContent = data.totalEntries || 0;
                                
                                if (data.levelDistribution) {
                                    document.getElementById('infoLogs').textContent = data.levelDistribution.INFO || 0;
                                    document.getElementById('warnLogs').textContent = data.levelDistribution.WARN || 0;
                                    document.getElementById('errorLogs').textContent = data.levelDistribution.ERROR || 0;
                                }
                                
                                updateStatsPanel(data);
                            })
                            .catch(error => {
                                console.error('Error loading stats:', error);
                                document.getElementById('statsPanel').innerHTML = '<p style="color: red;">Error loading statistics: ' + error.message + '</p>';
                            });
                    }

                    function loadRecentLogs() {
                        fetch('/api/recent?limit=15')
                            .then(response => {
                                if (!response.ok) throw new Error('API error: ' + response.status);
                                return response.json();
                            })
                            .then(data => {
                                console.log('Recent logs loaded:', data);
                                displayLogs(data.results || [], 'Recent Logs');
                            })
                            .catch(error => {
                                console.error('Error loading recent logs:', error);
                                document.getElementById('recentLogs').innerHTML = '<p style="color: red;">Error loading logs: ' + error.message + '</p>';
                            });
                    }

                    function performSearch() {
                        const query = document.getElementById('searchInput').value.trim();
                        if (query) {
                            fetch('/api/search?q=' + encodeURIComponent(query) + '&limit=20')
                                .then(response => {
                                    if (!response.ok) throw new Error('API error: ' + response.status);
                                    return response.json();
                                })
                                .then(data => {
                                    displayLogs(data.results || [], 'Search Results for "' + query + '"');
                                })
                                .catch(error => {
                                    console.error('Search error:', error);
                                    alert('Search failed: ' + error.message);
                                });
                        } else {
                            alert('Please enter a search query');
                        }
                    }

                    function displayLogs(logs, title) {
                        const logsPanel = document.getElementById('recentLogs');
                        
                        if (logs.length > 0) {
                            let html = '<h3>' + title + ' (' + logs.length + '):</h3>';
                            logs.forEach(log => {
                                html += createLogEntryHTML(log);
                            });
                            logsPanel.innerHTML = html;
                        } else {
                            logsPanel.innerHTML = '<h3>' + title + '</h3><p>No logs found.</p>';
                        }
                    }

                    function createLogEntryHTML(log) {
                        const time = new Date(log.timestamp).toLocaleTimeString();
                        let html = '<div class="log-entry ' + (log.level ? log.level.toLowerCase() : 'info') + '">';
                        html += '<strong>[' + time + ']</strong> ';
                        html += '<span style="color: #f59e0b">' + (log.source || 'unknown') + '</span> ';
                        html += '<span style="color: #0ea5e9">' + (log.level || 'INFO') + '</span> ';
                        html += '<div>' + (log.message || 'No message') + '</div>';
                        
                        if (log.fields && Object.keys(log.fields).length > 0) {
                            html += '<small style="color: #888">Fields: ' + JSON.stringify(log.fields) + '</small>';
                        }
                        
                        html += '</div>';
                        return html;
                    }

                    function updateStatsPanel(data) {
                        let statsHtml = '<div style="background: #16213e; padding: 15px; border-radius: 8px;">';
                        statsHtml += '<div><strong>Total Entries:</strong> ' + (data.totalEntries || 0) + '</div>';
                        
                        if (data.levelDistribution) {
                            statsHtml += '<h4 style="margin-top: 10px; margin-bottom: 5px;">Log Levels:</h4>';
                            for (const [level, count] of Object.entries(data.levelDistribution)) {
                                statsHtml += '<div>' + level + ': ' + count + '</div>';
                            }
                        }
                        
                        statsHtml += '<div style="margin-top: 10px; font-size: 12px; color: #888;">';
                        statsHtml += 'Last updated: ' + new Date().toLocaleTimeString();
                        statsHtml += '</div></div>';
                        
                        document.getElementById('statsPanel').innerHTML = statsHtml;
                    }

                    function testAPI() {
                        fetch('/api/health')
                            .then(response => response.json())
                            .then(data => {
                                alert('API Health Check:\\n' + JSON.stringify(data, null, 2));
                            })
                            .catch(error => {
                                alert('API Test Failed: ' + error.message);
                            });
                    }

                    function viewAllEndpoints() {
                        const endpoints = [
                            'GET /api/health - System health check',
                            'GET /api/stats - Get statistics', 
                            'GET /api/recent?limit=20 - Get recent logs',
                            'GET /api/search?q=query&limit=50 - Search logs'
                        ];
                        alert('Available Endpoints:\\n\\n' + endpoints.join('\\n'));
                    }

                    function clearLogs() {
                        document.getElementById('recentLogs').innerHTML = '<p>Logs cleared. Click "Refresh Logs" to load again.</p>';
                    }

                    // Auto-refresh stats every 10 seconds
                    setInterval(loadStats, 10000);
                    
                    // Load initial data
                    loadStats();
                    loadRecentLogs();
                    
                    console.log('Dashboard loaded successfully!');
                </script>
            </body>
            </html>
            """);
    }
}