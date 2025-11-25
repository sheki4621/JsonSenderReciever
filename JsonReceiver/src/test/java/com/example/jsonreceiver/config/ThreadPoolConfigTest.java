package com.example.jsonreceiver.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ThreadPoolConfigのテスト
 */
@SpringBootTest
@TestPropertySource(properties = {
        "notice.processing.thread-pool.size=5"
})
class ThreadPoolConfigTest {

    @Autowired
    private ExecutorService noticeProcessingExecutor;

    @Test
    void testExecutorServiceBeanIsCreated() {
        assertNotNull(noticeProcessingExecutor, "ExecutorServiceがDI可能であること");
        assertFalse(noticeProcessingExecutor.isShutdown(), "ExecutorServiceが起動していること");
    }

    @Test
    void testExecutorServiceCanSubmitTasks() throws Exception {
        // タスクが正常に実行できることを確認
        var future = noticeProcessingExecutor.submit(() -> {
            return "task completed";
        });

        String result = future.get();
        assertEquals("task completed", result, "タスクが正常に実行されること");
    }

    @Test
    void testThreadPoolSizeLimit() throws Exception {
        // ThreadPoolサイズは5に設定されている
        int expectedPoolSize = 5;
        int taskCount = 50;

        // 同時実行中のスレッド数を追跡
        AtomicInteger currentThreadCount = new AtomicInteger(0);
        AtomicInteger maxThreadCount = new AtomicInteger(0);

        // すべてのタスクが開始されるまで待機するためのラッチ
        CountDownLatch startLatch = new CountDownLatch(taskCount);
        // すべてのタスクが完了するまで待機するためのラッチ
        CountDownLatch completionLatch = new CountDownLatch(taskCount);

        List<Future<?>> futures = new ArrayList<>();

        // 大量のタスクを投入
        for (int i = 0; i < taskCount; i++) {
            Future<?> future = noticeProcessingExecutor.submit(() -> {
                try {
                    // スレッド数をインクリメント
                    int current = currentThreadCount.incrementAndGet();

                    // 最大スレッド数を更新
                    maxThreadCount.updateAndGet(max -> Math.max(max, current));

                    startLatch.countDown();

                    // 少し処理時間をかける（100ms）
                    Thread.sleep(100);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // スレッド数をデクリメント
                    currentThreadCount.decrementAndGet();
                    completionLatch.countDown();
                }
            });
            futures.add(future);
        }

        // すべてのタスクが完了するまで待機（最大10秒）
        assertTrue(completionLatch.await(10, TimeUnit.SECONDS),
                "すべてのタスクが完了すること");

        // すべてのタスクが正常に完了したことを確認
        for (Future<?> future : futures) {
            assertTrue(future.isDone(), "タスクが完了していること");
        }

        // 同時実行スレッド数が設定値を超えていないことを確認
        int observedMaxThreads = maxThreadCount.get();
        assertTrue(observedMaxThreads <= expectedPoolSize,
                String.format("同時実行スレッド数が設定値を超えていないこと（期待値: %d以下、実測値: %d）",
                        expectedPoolSize, observedMaxThreads));

        // 少なくともいくつかのタスクが並列実行されたことを確認
        assertTrue(observedMaxThreads >= 2,
                "複数のタスクが並列実行されたこと（実測値: " + observedMaxThreads + "）");
    }
}
