package com.example.jsonsender;

import com.example.jsoncommon.dto.*;
import com.example.jsonsender.config.AppConfig;
import com.example.jsonsender.service.MetricsSendService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RunnerTest {

    @Mock
    private TcpClient tcpClient;

    @Mock
    private MetricsSendService metricsSendService;

    @Mock
    private AppConfig appConfig;

    private Runner runner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // ネストされた設定オブジェクトのモック作成
        AppConfig.Dist dist = org.mockito.Mockito.mock(AppConfig.Dist.class);
        when(dist.getHostname()).thenReturn("localhost");
        when(dist.getPort()).thenReturn(8080);

        when(appConfig.getTimezone()).thenReturn("UTC");
        when(appConfig.getAgentVersion()).thenReturn("1.0.0");
        when(appConfig.getDist()).thenReturn(dist);
        when(appConfig.getNoticeIntervalSec()).thenReturn(1);
        when(appConfig.getErrorRetryIntervalSec()).thenReturn(5);

        runner = new Runner(tcpClient, metricsSendService, appConfig);
    }

    @Test
    void testRunSendsUpNotification() throws Exception {
        // Arrange
        // Throw Error to break the infinite loop in Runner.run()
        // collect() does not declare InterruptedException, so we cannot throw it.
        // catch(Exception) in Runner catches RuntimeException, so we use Error.
        when(metricsSendService.collect()).thenAnswer(invocation -> {
            throw new Error("Stop loop");
        });

        // Act
        try {
            runner.run();
        } catch (Error e) {
            if (!"Stop loop".equals(e.getMessage())) {
                throw e;
            }
        }

        // Assert
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(tcpClient).sendJson(anyString(), anyInt(), captor.capture());

        Object captured = captor.getValue();
        assertThat(captured).isInstanceOf(UpJson.class);
        UpJson notice = (UpJson) captured;
        assertThat(notice.getNoticeType()).isEqualTo(NoticeType.UP);
    }

    @Test
    void testOnExitSendsDownNotification() {
        // Act
        runner.onExit();

        // Assert
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(tcpClient).sendJsonDirectly(anyString(), anyInt(), captor.capture());

        Object captured = captor.getValue();
        assertThat(captured).isInstanceOf(DownJson.class);
        DownJson notice = (DownJson) captured;
        assertThat(notice.getNoticeType()).isEqualTo(NoticeType.DOWN);
    }
}
