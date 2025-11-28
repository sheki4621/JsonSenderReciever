package com.example.jsonsender.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JsonSenderのスレッドプール設定
 * 各種非同期処理で使用するExecutorを定義
 */
@Configuration
public class ThreadPoolConfig {

    @Value("${notice.processing.thread-pool.size:10}")
    private int threadPoolSize;

    @Value("${notice.processing.thread-pool.shutdown-timeout-seconds:30}")
    private int shutdownTimeoutSeconds;

    /**
     * NoticeType処理用の固定サイズThreadPool
     * TcpServerConfigで使用するExecutorService
     */
    @Bean(name = "noticeProcessingExecutor", destroyMethod = "shutdown")
    public ExecutorService noticeProcessingExecutor() {
        return Executors.newFixedThreadPool(threadPoolSize);
    }
}
