package com.example.jsonsender.config;

import com.example.jsoncommon.tcp.TcpServer;
import com.example.jsonsender.JsonSenderMessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JsonSenderのTCPサーバー設定
 */
@Configuration
@Slf4j
public class TcpServerConfig {

    @Value("${tcp.server.port:8888}")
    private int serverPort;

    @Value("${tcp.server.thread-pool-size:10}")
    private int threadPoolSize;

    @Bean(name = "jsonSenderTcpServerExecutor")
    public TaskExecutor jsonSenderTcpServerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("tcp-server-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "jsonSenderNoticeProcessingExecutor", destroyMethod = "shutdown")
    public ExecutorService jsonSenderNoticeProcessingExecutor() {
        return Executors.newFixedThreadPool(threadPoolSize);
    }

    @Bean
    public CommandLineRunner startTcpServer(
            JsonSenderMessageHandler messageHandler,
            ObjectMapper objectMapper,
            TaskExecutor jsonSenderTcpServerExecutor,
            ExecutorService jsonSenderNoticeProcessingExecutor) {

        return args -> {
            log.info("JsonSender TCPサーバーをポート{}で起動します", serverPort);
            // TcpServer(int port, MessageHandler messageHandler, ExecutorService
            // executorService, ObjectMapper objectMapper)
            TcpServer tcpServer = new TcpServer(
                    serverPort,
                    messageHandler,
                    jsonSenderNoticeProcessingExecutor,
                    objectMapper);

            jsonSenderTcpServerExecutor.execute(tcpServer);
        };
    }
}
