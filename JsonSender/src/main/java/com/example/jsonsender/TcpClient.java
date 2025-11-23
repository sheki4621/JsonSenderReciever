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

    public TcpClient() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void sendJson(String host, int port, Object data) {
        try (Socket socket = new Socket(host, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String json = objectMapper.writeValueAsString(data);
            logger.info("Sending JSON: {}", json);
            out.println(json);

        } catch (Exception e) {
            logger.error("Error sending JSON", e);
        }
    }
}
