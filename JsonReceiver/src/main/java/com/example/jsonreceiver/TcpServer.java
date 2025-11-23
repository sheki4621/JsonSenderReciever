package com.example.jsonreceiver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

@Component
public class TcpServer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(String... args) throws Exception {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(9999)) {
                logger.info("TCP Server started on port 9999");
                while (true) {
                    try (Socket clientSocket = serverSocket.accept();
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(clientSocket.getInputStream()))) {

                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            logger.info("Received raw: {}", inputLine);
                            try {
                                JsonNode jsonNode = objectMapper.readTree(inputLine);
                                logger.info("Parsed JSON: {}", jsonNode.toPrettyString());
                            } catch (Exception e) {
                                logger.error("Failed to parse JSON", e);
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
