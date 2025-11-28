package com.example.jsonsender.tcp;

import com.example.jsonsender.JsonFileManager;
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

    public TcpClient(com.example.jsonsender.config.AppConfig appConfig,
            JsonFileManager jsonFileManager,
            ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.jsonFileManager = jsonFileManager;
        this.objectMapper = objectMapper;
    }

    public void sendJson(String host, int port, Object data) {
        if (!sendJsonInternal(host, port, data)) {
            if (appConfig.getJson().isFailedArchive()) {
                jsonFileManager.save(data);
            }
        } else {
            if (appConfig.getJson().isFailedArchive()) {
                jsonFileManager.resendAsync();
            }
        }
    }

    public boolean sendJsonDirectly(String host, int port, Object data) {
        return sendJsonInternal(host, port, data);
    }

    private boolean sendJsonInternal(String host, int port, Object data) {
        int retryMax = appConfig.getSender().getRetryMax();
        int retryIntervalSec = appConfig.getSender().getRetryIntervalSec();
        int timeout = appConfig.getSender().getTimeout();

        for (int i = 0; i <= retryMax; i++) {
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), timeout * 1000);
                try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                    String json = objectMapper.writeValueAsString(data);
                    logger.info("JSONを送信します: {}", json);
                    out.println(json);
                    return true; // Success
                }
            } catch (Exception e) {
                logger.warn("JSON送信エラー (試行 {}/{}): {}", i + 1, retryMax + 1, e.getMessage());
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
        logger.error("{}回の試行後もJSON送信に失敗しました", retryMax + 1);
        return false;
    }
}
