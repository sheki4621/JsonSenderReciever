package com.example.jsonreceiver.service;

import com.example.jsoncommon.dto.Metrics;
import com.example.jsoncommon.dto.MetricsJson;
import com.example.jsoncommon.dto.NoticeType;
import com.example.jsonreceiver.repository.CsvRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MetricsServiceTest {

    @Mock
    private CsvRepository csvRepository;

    @Mock
    private ThresholdService thresholdService;

    private MetricsService metricsService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        metricsService = new MetricsService(csvRepository, thresholdService);
    }

    @Test
    public void testProcessMetrics() throws IOException {
        // Arrange
        Metrics metrics = new Metrics(75.0, 60.0);
        MetricsJson metricsJson = new MetricsJson(
                UUID.randomUUID(),
                NoticeType.METRICS,
                ZonedDateTime.now(),
                "1.0.0",
                "test-host",
                metrics);

        // Act
        metricsService.processMetrics(metricsJson);

        // Assert
        verify(csvRepository).saveResourceInfo(metricsJson);
        verify(thresholdService).checkThreshold(metricsJson);
    }

    @Test
    public void testProcessMetrics_CsvRepositoryThrowsException() throws IOException {
        // Arrange
        Metrics metrics = new Metrics(75.0, 60.0);
        MetricsJson metricsJson = new MetricsJson(
                UUID.randomUUID(),
                NoticeType.METRICS,
                ZonedDateTime.now(),
                "1.0.0",
                "test-host",
                metrics);

        doThrow(new IOException("Test exception")).when(csvRepository).saveResourceInfo(any(MetricsJson.class));

        // Act & Assert
        try {
            metricsService.processMetrics(metricsJson);
        } catch (RuntimeException e) {
            // Expected
        }

        verify(csvRepository).saveResourceInfo(metricsJson);
        verify(thresholdService, never()).checkThreshold(any(MetricsJson.class));
    }
}
