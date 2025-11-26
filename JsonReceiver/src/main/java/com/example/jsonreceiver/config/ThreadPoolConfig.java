package com.example.jsonreceiver.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * NoticeType処理用のThreadPool設定クラス
 * application.propertiesで指定されたサイズの固定ThreadPoolを作成します。
 */
@Configuration
public class ThreadPoolConfig {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolConfig.class);

    @Value("${notice.processing.thread-pool.size:10}")
    private int threadPoolSize;

    @Value("${notice.processing.thread-pool.shutdown-timeout-seconds:30}")
    private int shutdownTimeoutSeconds;

    private ExecutorService executorService;

    /**
     * NoticeType処理用のExecutorServiceを作成します
     * 
     * @return 固定サイズのThreadPoolExecutorService
     */
    @Bean(name = "noticeProcessingExecutor")
    public ExecutorService noticeProcessingExecutor() {
        logger.info("NoticeType 処理用のスレッドプールを作成します。サイズ: {}", threadPoolSize);
        executorService = Executors.newFixedThreadPool(threadPoolSize);
        return executorService;
    }

    /**
     * アプリケーション終了時にThreadPoolを適切にシャットダウンします
     */
    @PreDestroy
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            logger.info("NoticeType 処理用のスレッドプールをシャットダウンします (タイムアウト: {}秒)", shutdownTimeoutSeconds);
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                    logger.warn("スレッドプールが時間内に終了せず、強制的にシャットダウンします");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.error("スレッドプールのシャットダウンが中断されました", e);
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
