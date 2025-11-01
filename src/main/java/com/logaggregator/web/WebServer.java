package com.logaggregator.web;

import com.logaggregator.core.Config;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class WebServer {
    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);

    private final Server server;
    private final int port;

    public WebServer() {
        this.port = Config.getInt("web.server.port");
        this.server = new Server(new InetSocketAddress("localhost", port));

        setupContext();
    }

    private void setupContext() {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // ONLY use DashboardServlet for root path - remove DefaultServlet
        ServletHolder dashboardHolder = new ServletHolder("dashboard", new DashboardServlet());
        context.addServlet(dashboardHolder, "/");

        // API servlet for /api/* paths
        ServletHolder apiHolder = new ServletHolder("api", new LogApiServlet());
        context.addServlet(apiHolder, "/api/*");

        // Configure WebSocket
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.addMapping("/ws", LogWebSocket.class);
        });

        logger.debug("Web server context setup complete - using DashboardServlet only");
    }

    public void start() throws Exception {
        server.start();
        logger.info("✅ Web server started on http://localhost:{}", port);
        logger.info("✅ Dashboard available at http://localhost:{}", port);
        logger.info("✅ REST API available at http://localhost:{}/api/*", port);
    }

    public void stop() {
        if (server != null && server.isRunning()) {
            try {
                server.stop();
                logger.info("Web server stopped gracefully");
            } catch (Exception e) {
                logger.error("Error stopping web server", e);
            }
        }
    }

    public boolean isRunning() {
        return server != null && server.isRunning();
    }
}