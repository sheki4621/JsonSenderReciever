package com.example.jsonreceiver;

import com.example.jsonreceiver.dto.*;
import com.example.jsonreceiver.service.InstanceStatusService;
import com.example.jsonreceiver.service.MetricsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

@Component
@RequiredArgsConstructor
public class TcpServer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);
    private final MetricsService metricsService;
    private final InstanceStatusService instanceStatusService;
    private final ExecutorService noticeProcessingExecutor;
    private final ObjectMapper objectMapper;

    @Qualifier("tcpServerExecutor")
    private final TaskExecutor taskExecutor;

    @Value("${tcp.server.port:9999}")
    private int port;

    private int actualPort;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public int getPort() {
        return actualPort;
    }

    @Override
    public void run(String... args) throws Exception {
        taskExecutor.execute(() -> {
            try {
                serverSocket = new ServerSocket(port);
                this.actualPort = serverSocket.getLocalPort();
                logger.info("TCP Server started on port {}", actualPort);

                while (running) {
                    try (Socket clientSocket = serverSocket.accept();
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(clientSocket.getInputStream()))) {

                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            logger.info("Received raw: {}", inputLine);
                            try {
                                JsonNode jsonNode = objectMapper.readTree(inputLine);
                                if (jsonNode.has("NoticeType")) {
                                    String noticeTypeStr = jsonNode.get("NoticeType").asText();
                                    if (NoticeType.METRICS.name().equals(noticeTypeStr)) {
                                        MetricsJson metricsJson = objectMapper.treeToValue(jsonNode, MetricsJson.class);
                                        noticeProcessingExecutor.submit(() -> {
                                            try {
                                                metricsService.processMetrics(metricsJson);
                                                logger.info("Processed METRICS: {}", metricsJson.getId());
                                            } catch (Exception e) {
                                                logger.error("Failed to process METRICS: {}", metricsJson.getId(), e);
                                            }
                                        });
                                    } else if (NoticeType.INSTALL.name().equals(noticeTypeStr)) {
                                        InstallJson installJson = objectMapper.treeToValue(jsonNode, InstallJson.class);
                                        noticeProcessingExecutor.submit(() -> {
                                            try {
                                                instanceStatusService.processInstall(installJson);
                                                logger.info("Processed INSTALL: {}", installJson.getId());
                                            } catch (Exception e) {
                                                logger.error("Failed to process INSTALL: {}", installJson.getId(), e);
                                            }
                                        });
                                    } else if (NoticeType.UNINSTALL.name().equals(noticeTypeStr)) {
                                        UninstallJson uninstallJson = objectMapper.treeToValue(jsonNode,
                                                UninstallJson.class);
                                        noticeProcessingExecutor.submit(() -> {
                                            try {
                                                instanceStatusService.processUninstall(uninstallJson);
                                                logger.info("Processed UNINSTALL: {}", uninstallJson.getId());
                                            } catch (Exception e) {
                                                logger.error("Failed to process UNINSTALL: {}", uninstallJson.getId(),
                                                        e);
                                            }
                                        });
                                    } else if (NoticeType.UP.name().equals(noticeTypeStr)) {
                                        UpJson upJson = objectMapper.treeToValue(jsonNode, UpJson.class);
                                        noticeProcessingExecutor.submit(() -> {
                                            try {
                                                instanceStatusService.processUp(upJson);
                                                logger.info("Processed UP: {}", upJson.getId());
                                            } catch (Exception e) {
                                                logger.error("Failed to process UP: {}", upJson.getId(), e);
                                            }
                                        });
                                    } else if (NoticeType.DOWN.name().equals(noticeTypeStr)) {
                                        DownJson downJson = objectMapper.treeToValue(jsonNode, DownJson.class);
                                        noticeProcessingExecutor.submit(() -> {
                                            try {
                                                instanceStatusService.processDown(downJson);
                                                logger.info("Processed DOWN: {}", downJson.getId());
                                            } catch (Exception e) {
                                                logger.error("Failed to process DOWN: {}", downJson.getId(), e);
                                            }
                                        });
                                    } else {
                                        logger.info("Ignored NoticeType: {}", noticeTypeStr);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("Failed to parse or process JSON", e);
                            }
                        }
                    } catch (Exception e) {
                        if (running) {
                            logger.error("Error handling client", e);
                        }
                    }
                }
            } catch (Exception e) {
                if (running) {
                    logger.error("Error starting server", e);
                }
            } finally {
                closeServerSocket();
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down TCP Server");
        running = false;
        closeServerSocket();
    }

    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                logger.info("ServerSocket closed successfully");
            } catch (Exception e) {
                logger.error("Error closing ServerSocket", e);
            }
        }
    }
}
