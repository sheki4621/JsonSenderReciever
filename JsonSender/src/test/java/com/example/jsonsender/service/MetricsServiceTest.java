package com.example.jsonsender.service;

import com.example.jsoncommon.dto.InstanceTypeChangeRequest;
import com.example.jsoncommon.dto.Metrics;
import com.example.jsoncommon.repository.ResourceHistoryRepository;
import com.example.jsonsender.repository.ThresholdRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock
    private ThresholdRepository thresholdRepository;

    @Mock
    private ResourceHistoryRepository resourceHistoryRepository;

    private MetricsSendService metricsSendService;

    @BeforeEach
    void setUp() {
        metricsSendService = spy(new MetricsSendService(thresholdRepository, resourceHistoryRepository));
    }

    @Test
    void testCollect_Success() throws Exception {
        // Given: しきい値チェックが正常に動作
        doReturn(InstanceTypeChangeRequest.UPPER).when(metricsSendService).getInstanceTypeChangeRequest(anyDouble(),
                anyDouble());

        // When: メトリクス収集
        Metrics metrics = metricsSendService.collect();

        // Then: 正しいメトリクスが返されることを確認
        assertNotNull(metrics);
        assertNotNull(metrics.getCpuUsage());
        assertNotNull(metrics.getMemoryUsage());
        assertEquals(InstanceTypeChangeRequest.UPPER, metrics.getInstanceTypeChangeRequest());

        // しきい値チェックが呼ばれたことを確認
        verify(metricsSendService, times(1)).getInstanceTypeChangeRequest(anyDouble(), anyDouble());
    }

    @Test
    void testCollect_ThresholdCheckFailure() throws Exception {
        // Given: しきい値チェックが例外をスロー
        doThrow(new RuntimeException("しきい値チェックエラー")).when(metricsSendService).getInstanceTypeChangeRequest(anyDouble(),
                anyDouble());

        // When: メトリクス収集
        Metrics metrics = metricsSendService.collect();

        // Then: instanceTypeChangeRequestがnullになることを確認
        assertNotNull(metrics);
        assertNotNull(metrics.getCpuUsage());
        assertNotNull(metrics.getMemoryUsage());
        assertNull(metrics.getInstanceTypeChangeRequest(), "しきい値チェック失敗時はnullになるべきです");

        // しきい値チェックが呼ばれたことを確認
        verify(metricsSendService, times(1)).getInstanceTypeChangeRequest(anyDouble(), anyDouble());
    }

    @Test
    void testCollect_WithinThreshold() throws Exception {
        // Given: しきい値内
        doReturn(InstanceTypeChangeRequest.WITHIN).when(metricsSendService).getInstanceTypeChangeRequest(anyDouble(),
                anyDouble());

        // When: メトリクス収集
        Metrics metrics = metricsSendService.collect();

        // Then: WITHINが返されることを確認
        assertNotNull(metrics);
        assertEquals(InstanceTypeChangeRequest.WITHIN, metrics.getInstanceTypeChangeRequest());
    }

    @Test
    void testCollect_LowerThreshold() throws Exception {
        // Given: 下限しきい値
        doReturn(InstanceTypeChangeRequest.LOWER).when(metricsSendService).getInstanceTypeChangeRequest(anyDouble(),
                anyDouble());

        // When: メトリクス収集
        Metrics metrics = metricsSendService.collect();

        // Then: LOWERが返されることを確認
        assertNotNull(metrics);
        assertEquals(InstanceTypeChangeRequest.LOWER, metrics.getInstanceTypeChangeRequest());
    }
}
