package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.*;
import com.example.jsonreceiver.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ThresholdServiceTest {

    @Mock
    private ResourceInfoRepository resourceInfoRepository;

    @Mock
    private ThresholdRepository thresholdRepository;

    @Mock
    private InstanceTypeChangeService instanceTypeChangeService;

    private ThresholdService thresholdService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        thresholdService = new ThresholdService(
                resourceInfoRepository,
                thresholdRepository,
                instanceTypeChangeService);
    }

    @Test
    public void testCheckThreshold_NoThresholdFound() throws IOException {
        // Arrange
        MetricsJson metricsJson = createMetricsJson("test-host", 80.0, 75.0);
        when(thresholdRepository.findByHostname("test-host")).thenReturn(Optional.empty());

        // Act
        thresholdService.checkThreshold(metricsJson);

        // Assert
        verify(thresholdRepository).findByHostname("test-host");
        verify(instanceTypeChangeService, never()).changeInstanceType(anyString(), anyString());
    }

    @Test
    public void testCheckThreshold_WithinLimits() throws IOException {
        // Arrange
        MetricsJson metricsJson = createMetricsJson("test-host", 50.0, 50.0);
        ThresholdInfo threshold = new ThresholdInfo("test-host", 80.0, 20.0, 85.0, 25.0, 3);
        when(thresholdRepository.findByHostname("test-host")).thenReturn(Optional.of(threshold));

        // Act
        thresholdService.checkThreshold(metricsJson);

        // Assert
        verify(thresholdRepository).findByHostname("test-host");
        verify(resourceInfoRepository, never()).findLastNByHostname(anyString(), anyInt());
        verify(instanceTypeChangeService, never()).changeInstanceType(anyString(), anyString());
    }

    @Test
    public void testCheckThreshold_CpuExceedsUpperLimit() throws IOException {
        // Arrange
        MetricsJson metricsJson = createMetricsJson("test-host", 85.0, 50.0);
        ThresholdInfo threshold = new ThresholdInfo("test-host", 80.0, 20.0, 85.0, 25.0, 3);

        // 過去2回のデータも上限超過
        List<ResourceInfo> history = Arrays.asList(
                new ResourceInfo("test-host", "2025-11-26T00:02:00+09:00", 83.0, 50.0),
                new ResourceInfo("test-host", "2025-11-26T00:01:00+09:00", 82.0, 50.0),
                new ResourceInfo("test-host", "2025-11-26T00:00:00+09:00", 81.0, 50.0));

        when(thresholdRepository.findByHostname("test-host")).thenReturn(Optional.of(threshold));
        when(resourceInfoRepository.findLastNByHostname("test-host", 3 - 1)).thenReturn(history);

        // Act
        thresholdService.checkThreshold(metricsJson);

        // Assert
        verify(thresholdRepository).findByHostname("test-host");
        verify(resourceInfoRepository).findLastNByHostname("test-host", 2);
        verify(instanceTypeChangeService).changeInstanceType("test-host", "HIGH");
    }

    @Test
    public void testCheckThreshold_MemoryBelowLowerLimit() throws IOException {
        // Arrange
        MetricsJson metricsJson = createMetricsJson("test-host", 50.0, 20.0);
        ThresholdInfo threshold = new ThresholdInfo("test-host", 80.0, 20.0, 85.0, 25.0, 3);

        // 過去2回のデータも下限未満
        List<ResourceInfo> history = Arrays.asList(
                new ResourceInfo("test-host", "2025-11-26T00:02:00+09:00", 50.0, 22.0),
                new ResourceInfo("test-host", "2025-11-26T00:01:00+09:00", 50.0, 21.0));

        when(thresholdRepository.findByHostname("test-host")).thenReturn(Optional.of(threshold));
        when(resourceInfoRepository.findLastNByHostname("test-host", 2)).thenReturn(history);

        // Act
        thresholdService.checkThreshold(metricsJson);

        // Assert
        verify(thresholdRepository).findByHostname("test-host");
        verify(resourceInfoRepository).findLastNByHostname("test-host", 2);
        verify(instanceTypeChangeService).changeInstanceType("test-host", "LOW");
    }

    @Test
    public void testCheckThreshold_NotContinuous() throws IOException {
        // Arrange
        MetricsJson metricsJson = createMetricsJson("test-host", 85.0, 50.0);
        ThresholdInfo threshold = new ThresholdInfo("test-host", 80.0, 20.0, 85.0, 25.0, 3);

        // 過去データに正常値が混在
        List<ResourceInfo> history = Arrays.asList(
                new ResourceInfo("test-host", "2025-11-26T00:02:00+09:00", 75.0, 50.0), // 正常
                new ResourceInfo("test-host", "2025-11-26T00:01:00+09:00", 82.0, 50.0));

        when(thresholdRepository.findByHostname("test-host")).thenReturn(Optional.of(threshold));
        when(resourceInfoRepository.findLastNByHostname("test-host", 2)).thenReturn(history);

        // Act
        thresholdService.checkThreshold(metricsJson);

        // Assert
        verify(thresholdRepository).findByHostname("test-host");
        verify(resourceInfoRepository).findLastNByHostname("test-host", 2);
        verify(instanceTypeChangeService, never()).changeInstanceType(anyString(), anyString());
    }

    @Test
    public void testCheckThreshold_InsufficientHistory() throws IOException {
        // Arrange
        MetricsJson metricsJson = createMetricsJson("test-host", 85.0, 50.0);
        ThresholdInfo threshold = new ThresholdInfo("test-host", 80.0, 20.0, 85.0, 25.0, 3);

        // 過去データが1件のみ（継続回数3だが過去データが不足）
        List<ResourceInfo> history = Arrays.asList(
                new ResourceInfo("test-host", "2025-11-26T00:01:00+09:00", 82.0, 50.0));

        when(thresholdRepository.findByHostname("test-host")).thenReturn(Optional.of(threshold));
        when(resourceInfoRepository.findLastNByHostname("test-host", 2)).thenReturn(history);

        // Act
        thresholdService.checkThreshold(metricsJson);

        // Assert
        verify(thresholdRepository).findByHostname("test-host");
        verify(resourceInfoRepository).findLastNByHostname("test-host", 2);
        // 過去データ不足なのでインスタンスタイプ変更は実行されない
        verify(instanceTypeChangeService, never()).changeInstanceType(anyString(), anyString());
    }

    private MetricsJson createMetricsJson(String hostname, double cpuUsage, double memoryUsage) {
        Metrics metrics = new Metrics(cpuUsage, memoryUsage);
        return new MetricsJson(
                UUID.randomUUID(),
                NoticeType.METRICS,
                ZonedDateTime.now(),
                "1.0.0",
                hostname,
                metrics);
    }
}
