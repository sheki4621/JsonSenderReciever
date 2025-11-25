package com.example.jsonreceiver;

import com.example.jsonreceiver.dto.MetricsJson;
import com.example.jsonreceiver.dto.NoticeType;
import com.example.jsonreceiver.service.MetricsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

@Component
@RequiredArgsConstructor
public class TcpServer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @org.springframework.beans.factory.annotation.Value("${tcp.server.port:9999}")
    private int port;

    private int actualPort;

    public int getPort() {
        return actualPort;
    }

    @Override
    public void run(String... args) throws Exception {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                this.actualPort = serverSocket.getLocalPort();
                logger.info("TCP Server started on port {}", actualPort);
                while (true) {
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
                                        metricsService.processMetrics(metricsJson);
                                        logger.info("Processed METRICS: {}", metricsJson.getId());
                                    } else {
                                        logger.info("Ignored NoticeType: {}", noticeTypeStr);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("Failed to parse or process JSON", e);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Error handling client", e);
                    }
                }
            } catch (Exception e) {
                logger.error("Error starting server", e);
            }
        }).start();
    }
}
