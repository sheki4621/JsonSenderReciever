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

    public TcpClient(com.example.jsonsender.config.AppConfig appConfig) {
        this.appConfig = appConfig;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void sendJson(String host, int port, Object data) {
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
                    return; // Success
                }
            } catch (Exception e) {
                logger.error("Error sending JSON (Attempt {}/{}): {}", i + 1, retryMax + 1, e.getMessage());
                if (i < retryMax) {
                    try {
                        Thread.sleep(retryIntervalSec * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
        logger.error("Failed to send JSON after {} attempts", retryMax + 1);
    }
}
