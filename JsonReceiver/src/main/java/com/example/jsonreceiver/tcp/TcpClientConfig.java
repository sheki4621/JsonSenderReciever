package com.example.jsonreceiver.tcp;

import com.example.jsoncommon.tcp.TcpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TcpClientConfig {

    @Bean
    public TcpClient tcpClient(ObjectMapper objectMapper) {
        return new TcpClient(objectMapper);
    }
}
