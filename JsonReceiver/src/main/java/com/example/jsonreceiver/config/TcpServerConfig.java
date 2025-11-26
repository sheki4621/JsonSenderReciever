package com.example.jsonreceiver.config;

import com.example.jsoncommon.tcp.TcpServer;
import com.example.jsonreceiver.JsonReceiverMessageHandler;
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
 * JsonReceiverのTCPサーバー設定
 */
@Configuration
@Slf4j
public class TcpServerConfig {

    @Value("${tcp.server.port:9999}")
    private int serverPort;

    @Value("${notice.processing.thread-pool.size:10}")
    private int threadPoolSize;

    @Bean(name = "jsonReceiverTcpServerExecutor")
    public TaskExecutor jsonReceiverTcpServerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("tcp-server-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "jsonReceiverNoticeProcessingExecutor", destroyMethod = "shutdown")
    public ExecutorService jsonReceiverNoticeProcessingExecutor() {
        return Executors.newFixedThreadPool(threadPoolSize);
    }

    @Bean
    public CommandLineRunner startJsonReceiverTcpServer(
            JsonReceiverMessageHandler messageHandler,
            ObjectMapper objectMapper,
            TaskExecutor jsonReceiverTcpServerExecutor,
            ExecutorService jsonReceiverNoticeProcessingExecutor) {

        return args -> {
            log.info("JsonReceiver TCPサーバーをポート{}で起動します", serverPort);
            // TcpServer(int port, MessageHandler messageHandler, ExecutorService
            // executorService, ObjectMapper objectMapper)
            TcpServer tcpServer = new TcpServer(
                    serverPort,
                    messageHandler,
                    jsonReceiverNoticeProcessingExecutor,
                    objectMapper);

            jsonReceiverTcpServerExecutor.execute(tcpServer);
        };
    }
}
