package com.example.jsonsender;

import com.example.jsonsender.config.AppConfig;
import com.example.jsonsender.metrics.Metrics;
import com.example.jsonsender.utils.collector.Collector;
import com.example.jsonsender.utils.notice.FinJson;
import com.example.jsonsender.utils.notice.InitJson;
import com.example.jsonsender.utils.notice.NoticeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RunnerTest {

    @Mock
    private TcpClient tcpClient;

    @Mock
    private Collector<Metrics> metricsCollector;

    @Mock
    private AppConfig appConfig;

    private Runner runner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(appConfig.getTimezone()).thenReturn("UTC");
        when(appConfig.getAgentVersion()).thenReturn("1.0.0");
        when(appConfig.getDistHostname()).thenReturn("localhost");
        when(appConfig.getDistPort()).thenReturn(8080);
        when(appConfig.getNoticeIntervalSec()).thenReturn(1);

        runner = new Runner(tcpClient, metricsCollector, appConfig);
    }

    @Test
    void testRunSendsInitNotification() throws Exception {
        // Arrange
        // Throw InterruptedException to break the loop immediately after first
        // iteration logic (or before)
        // Actually, we want it to run at least once to send INIT?
        // INIT is sent BEFORE the loop.
        // So we can just throw InterruptedException immediately in
        // metricsCollector.collect()
        when(metricsCollector.collect()).thenThrow(new InterruptedException("Stop loop"));

        // Act
        runner.run();

        // Assert
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(tcpClient).sendJson(anyString(), anyInt(), captor.capture());

        Object captured = captor.getValue();
        assertThat(captured).isInstanceOf(InitJson.class);
        InitJson notice = (InitJson) captured;
        assertThat(notice.getNoticeType()).isEqualTo(NoticeType.INIT);
    }

    @Test
    void testOnExitSendsFinNotification() {
        // Act
        runner.onExit();

        // Assert
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(tcpClient).sendJsonDirectly(anyString(), anyInt(), captor.capture());

        Object captured = captor.getValue();
        assertThat(captured).isInstanceOf(FinJson.class);
        FinJson notice = (FinJson) captured;
        assertThat(notice.getNoticeType()).isEqualTo(NoticeType.FIN);
    }
}
