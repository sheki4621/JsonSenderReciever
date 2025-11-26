package com.example.jsonreceiver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JsonReceiverのスレッドプール設定
 * 各種非同期処理で使用するExecutorを定義
 */
@Configuration
public class ThreadPoolConfig {

    @Value("${notice.processing.thread-pool.size:10}")
    private int threadPoolSize;

    @Value("${notice.processing.thread-pool.shutdown-timeout-seconds:30}")
    private int shutdownTimeoutSeconds;

    /**
     * 情報収集用のTaskExecutor
     * 単一スレッドで定期的な情報収集を実行
     */
    @Bean(name = "infoCollectionExecutor")
    public TaskExecutor infoCollectionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("info-collection-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(shutdownTimeoutSeconds);
        executor.initialize();
        return executor;
    }

    /**
     * NoticeType処理用の固定サイズThreadPool
     * TcpServerConfigで使用するExecutorService
     */
    @Bean(name = "noticeProcessingExecutor", destroyMethod = "shutdown")
    public ExecutorService noticeProcessingExecutor() {
        return Executors.newFixedThreadPool(threadPoolSize);
    }
}
