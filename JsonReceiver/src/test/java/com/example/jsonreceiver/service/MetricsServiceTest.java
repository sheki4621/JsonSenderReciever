package com.example.jsonreceiver.service;

import com.example.jsoncommon.dto.InstanceTypeChangeRequest;
import com.example.jsoncommon.dto.Metrics;
import com.example.jsoncommon.dto.MetricsJson;
import com.example.jsoncommon.dto.NoticeType;
import com.example.jsoncommon.repository.ResourceHistoryRepository;
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
    private ResourceHistoryRepository resourceHistoryRepository;

    private MetricsService metricsService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        metricsService = new MetricsService(resourceHistoryRepository);
    }

    @Test
    public void testProcessMetrics() throws IOException {
        // Arrange
        Metrics metrics = new Metrics(75.0, 60.0, InstanceTypeChangeRequest.WITHIN);
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
        verify(resourceHistoryRepository).save(metricsJson);
    }

    @Test
    public void testProcessMetrics_CsvRepositoryThrowsException() throws IOException {
        // Arrange
        Metrics metrics = new Metrics(75.0, 60.0, InstanceTypeChangeRequest.WITHIN);
        MetricsJson metricsJson = new MetricsJson(
                UUID.randomUUID(),
                NoticeType.METRICS,
                ZonedDateTime.now(),
                "1.0.0",
                "test-host",
                metrics);

        doThrow(new IOException("Test exception")).when(resourceHistoryRepository).save(any(MetricsJson.class));

        // Act & Assert
        try {
            metricsService.processMetrics(metricsJson);
        } catch (RuntimeException e) {
            // Expected
        }

        verify(resourceHistoryRepository).save(metricsJson);
    }
}
