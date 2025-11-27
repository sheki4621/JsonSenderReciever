package com.example.jsoncommon.tcp;

import com.example.jsoncommon.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TcpClientServerIntegrationTest {

    private ObjectMapper objectMapper;
    private ExecutorService serverExecutor;
    private ExecutorService messageHandlerExecutor;
    private TcpServer tcpServer;
    private TcpClient tcpClient;
    private TestMessageHandler messageHandler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        serverExecutor = Executors.newSingleThreadExecutor();
        messageHandlerExecutor = Executors.newFixedThreadPool(4);
        messageHandler = new TestMessageHandler();
    }

    @AfterEach
    void tearDown() {
        if (tcpServer != null) {
            tcpServer.shutdown();
        }
        if (serverExecutor != null) {
            serverExecutor.shutdownNow();
        }
        if (messageHandlerExecutor != null) {
            messageHandlerExecutor.shutdownNow();
        }
    }

    @Test
    void testSendAndReceiveMetricsJson() throws Exception {
        // Arrange
        tcpServer = new TcpServer(0, messageHandler, messageHandlerExecutor, objectMapper);
        serverExecutor.submit(tcpServer);
        Thread.sleep(1000); // サーバー起動待機

        int port = tcpServer.getPort();
        tcpClient = new TcpClient(objectMapper);

        UUID id = UUID.randomUUID();
        Metrics metrics = new Metrics(75.5, 60.3, InstanceTypeChangeRequest.WITHIN);
        MetricsJson metricsJson = new MetricsJson(
                id, NoticeType.METRICS, ZonedDateTime.now(), "1.0.0", "test-instance", metrics);

        // Act
        TcpConfig config = TcpConfig.builder().timeout(5).retryMax(3).retryIntervalSec(1).build();
        boolean result = tcpClient.sendJson("localhost", port, metricsJson, config);

        // Assert
        assertTrue(result, "送信が成功すること");
        assertTrue(messageHandler.waitForMessage(5, TimeUnit.SECONDS), "メッセージが受信されること");
        assertEquals(1, messageHandler.getReceivedMessages().size(), "1つのメッセージを受信すること");

        NoticeBaseJson received = messageHandler.getReceivedMessages().get(0);
        assertTrue(received instanceof MetricsJson, "MetricsJsonとして受信されること");
        MetricsJson receivedMetrics = (MetricsJson) received;
        assertEquals(id, receivedMetrics.getId());
        assertEquals(NoticeType.METRICS, receivedMetrics.getNoticeType());
        assertEquals(75.5, receivedMetrics.getMetrics().getCpuUsage());
        assertEquals(60.3, receivedMetrics.getMetrics().getMemoryUsage());
    }

    @Test
    void testSendUpJson() throws Exception {
        // Arrange
        tcpServer = new TcpServer(0, messageHandler, messageHandlerExecutor, objectMapper);
        serverExecutor.submit(tcpServer);
        Thread.sleep(1000);

        int port = tcpServer.getPort();
        tcpClient = new TcpClient(objectMapper);

        UUID id = UUID.randomUUID();
        UpJson upJson = new UpJson(id, ZonedDateTime.now(), "1.0.0", "test-instance");

        // Act
        TcpConfig config = TcpConfig.builder().build();
        boolean result = tcpClient.sendJson("localhost", port, upJson, config);

        // Assert
        assertTrue(result);
        assertTrue(messageHandler.waitForMessage(5, TimeUnit.SECONDS));
        NoticeBaseJson received = messageHandler.getReceivedMessages().get(0);
        assertTrue(received instanceof UpJson);
        assertEquals(NoticeType.UP, received.getNoticeType());
    }

    @Test
    void testSendFailureCallback() {
        // Arrange
        tcpClient = new TcpClient(objectMapper);
        TestSendFailureCallback callback = new TestSendFailureCallback();
        UpJson upJson = new UpJson(UUID.randomUUID(), ZonedDateTime.now(), "1.0.0", "test");

        // Act - 接続不可能なポートに送信
        TcpConfig config = TcpConfig.builder().timeout(1).retryMax(0).build();
        boolean result = tcpClient.sendJsonWithCallback("localhost", 9, upJson, config, callback);

        // Assert
        assertFalse(result, "送信が失敗すること");
        assertTrue(callback.isCallbackInvoked(), "コールバックが呼び出されること");
        assertNotNull(callback.getFailedData());
    }

    // テスト用MessageHandler実装
    private static class TestMessageHandler implements MessageHandler {
        private final List<NoticeBaseJson> receivedMessages = new ArrayList<>();
        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public synchronized void handleMessage(NoticeBaseJson message) {
            receivedMessages.add(message);
            latch.countDown();
        }

        public synchronized List<NoticeBaseJson> getReceivedMessages() {
            return new ArrayList<>(receivedMessages);
        }

        public boolean waitForMessage(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
    }

    // テスト用SendFailureCallback実装
    private static class TestSendFailureCallback implements SendFailureCallback {
        private boolean callbackInvoked = false;
        private Object failedData;

        @Override
        public void onSendFailure(Object data) {
            this.callbackInvoked = true;
            this.failedData = data;
        }

        public boolean isCallbackInvoked() {
            return callbackInvoked;
        }

        public Object getFailedData() {
            return failedData;
        }
    }
}
