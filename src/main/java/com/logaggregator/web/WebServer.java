package com.logaggregator.web;

import com.logaggregator.core.Config;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URL;

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

        // Add REST API servlet
        ServletHolder apiServlet = new ServletHolder("api", new LogApiServlet());
        context.addServlet(apiServlet, "/api/*");

        // Serve static files from classpath
        context.setResourceBase(getResourceBase());
        context.addServlet(new ServletHolder("default", new org.eclipse.jetty.servlet.DefaultServlet()), "/");

        // Configure WebSocket
        JettyWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
            wsContainer.addMapping("/ws", LogWebSocket.class);
        });

        logger.debug("Web server context setup complete");
    }

    private String getResourceBase() {
        // Try to find the web resources in classpath
        URL webResource = getClass().getClassLoader().getResource("web");
        if (webResource != null) {
            return webResource.toExternalForm();
        }

        // Fallback to current directory
        logger.warn("Web resources not found in classpath, using current directory");
        return ".";
    }

    public void start() throws Exception {
        server.start();
        logger.info("Web server started on http://localhost:{}", port);
        logger.info("Dashboard available at http://localhost:{}", port);
        logger.info("WebSocket available on ws://localhost:{}/ws", port);
        logger.info("REST API available on http://localhost:{}/api/*", port);
    }

    public void stop() throws Exception {
        server.stop();
        logger.info("Web server stopped");
    }

    public boolean isRunning() {
        return server.isRunning();
    }
}