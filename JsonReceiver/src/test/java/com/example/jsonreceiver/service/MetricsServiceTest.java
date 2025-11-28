package com.example.jsonreceiver.service;

import com.example.jsoncommon.dto.InstanceTypeChangeRequest;
import com.example.jsoncommon.dto.Metrics;
import com.example.jsoncommon.dto.MetricsJson;
import com.example.jsoncommon.dto.NoticeType;
import com.example.jsoncommon.repository.ResourceHistoryRepository;
import com.example.jsonreceiver.dto.InstanceType;
import com.example.jsonreceiver.repository.InstanceStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class MetricsServiceTest {

    @Mock
    private ResourceHistoryRepository resourceHistoryRepository;

    @Mock
    private InstanceStatusRepository instanceStatusRepository;

    @Mock
    private InstanceTypeChangeService instanceTypeChangeService;

    private MetricsService metricsService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        metricsService = new MetricsService(resourceHistoryRepository, instanceStatusRepository,
                instanceTypeChangeService);
    }

    @Test
    public void testProcessMetrics_WithinRequest() throws IOException {
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
        verify(instanceStatusRepository).updateAgentLastNoticeTime(eq("test-host"), anyString());
        // WITHIN なので changeInstanceType は呼ばれない
        verify(instanceTypeChangeService, never()).changeInstanceType(anyString(), any(InstanceType.class));
    }

    @Test
    public void testProcessMetrics_UpperRequest() throws IOException {
        // Arrange
        Metrics metrics = new Metrics(75.0, 60.0, InstanceTypeChangeRequest.UPPER);
        MetricsJson metricsJson = new MetricsJson(
                UUID.randomUUID(),
                NoticeType.METRICS,
                ZonedDateTime.now(),
                "1.0.0",
                "test-host-upper",
                metrics);

        // Act
        metricsService.processMetrics(metricsJson);

        // Assert
        verify(resourceHistoryRepository).save(metricsJson);
        verify(instanceStatusRepository).updateAgentLastNoticeTime(eq("test-host-upper"), anyString());
        verify(instanceTypeChangeService).changeInstanceType("test-host-upper", InstanceType.HIGH);
    }

    @Test
    public void testProcessMetrics_LowerRequest() throws IOException {
        // Arrange
        Metrics metrics = new Metrics(75.0, 60.0, InstanceTypeChangeRequest.LOWER);
        MetricsJson metricsJson = new MetricsJson(
                UUID.randomUUID(),
                NoticeType.METRICS,
                ZonedDateTime.now(),
                "1.0.0",
                "test-host-lower",
                metrics);

        // Act
        metricsService.processMetrics(metricsJson);

        // Assert
        verify(resourceHistoryRepository).save(metricsJson);
        verify(instanceStatusRepository).updateAgentLastNoticeTime(eq("test-host-lower"), anyString());
        verify(instanceTypeChangeService).changeInstanceType("test-host-lower", InstanceType.LOW);
    }

    @Test
    public void testProcessMetrics_NullRequest() throws IOException {
        // Arrange
        Metrics metrics = new Metrics(75.0, 60.0, null);
        MetricsJson metricsJson = new MetricsJson(
                UUID.randomUUID(),
                NoticeType.METRICS,
                ZonedDateTime.now(),
                "1.0.0",
                "test-host-null",
                metrics);

        // Act
        metricsService.processMetrics(metricsJson);

        // Assert
        verify(resourceHistoryRepository).save(metricsJson);
        verify(instanceStatusRepository).updateAgentLastNoticeTime(eq("test-host-null"), anyString());
        // null なので changeInstanceType は呼ばれない
        verify(instanceTypeChangeService, never()).changeInstanceType(anyString(), any(InstanceType.class));
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
