package com.example.jsonsender;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.net.Socket;

@Component
public class TcpClient {

    private static final Logger logger = LoggerFactory.getLogger(TcpClient.class);
    private final ObjectMapper objectMapper;
    private final com.example.jsonsender.config.AppConfig appConfig;
    private final JsonFileManager jsonFileManager;

    public TcpClient(com.example.jsonsender.config.AppConfig appConfig, JsonFileManager jsonFileManager) {
        this.appConfig = appConfig;
        this.jsonFileManager = jsonFileManager;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void sendJson(String host, int port, Object data) {
        if (!sendJsonInternal(host, port, data)) {
            if (appConfig.isFailedArchive()) {
                jsonFileManager.save(data);
            }
        } else {
            if (appConfig.isFailedArchive()) {
                jsonFileManager.resendAsync();
            }
        }
    }

    public boolean sendJsonDirectly(String host, int port, Object data) {
        return sendJsonInternal(host, port, data);
    }

    private boolean sendJsonInternal(String host, int port, Object data) {
        int retryMax = appConfig.getRetryMax();
        int retryIntervalSec = appConfig.getRetryIntervalSec();
        int timeout = appConfig.getTimeout();

        for (int i = 0; i <= retryMax; i++) {
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), timeout * 1000);
                try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                    String json = objectMapper.writeValueAsString(data);
                    logger.info("Sending JSON: {}", json);
                    out.println(json);
                    return true; // Success
                }
            } catch (Exception e) {
                logger.warn("Error sending JSON (Attempt {}/{}): {}", i + 1, retryMax + 1, e.getMessage());
                if (i < retryMax) {
                    try {
                        Thread.sleep(retryIntervalSec * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        logger.error("Failed to send JSON after {} attempts", retryMax + 1);
        return false;
    }
}
