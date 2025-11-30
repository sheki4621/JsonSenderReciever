package com.example.jsonreceiver.metrics;

import com.example.jsoncommon.dto.InstanceTypeChangeRequest;
import com.example.jsoncommon.dto.Metrics;
import com.example.jsoncommon.dto.MetricsJson;
import com.example.jsoncommon.dto.NoticeType;
import com.example.jsoncommon.repository.ResourceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceInfoRepositoryTest {

    @TempDir
    Path tempDir;

    private ResourceHistoryRepository repository;
    private Path csvFilePath;

    @BeforeEach
    public void setUp() throws IOException {
        Path csvDir = tempDir.resolve("csv");
        Files.createDirectories(csvDir);
        csvFilePath = csvDir.resolve("resource_history_test-host.csv");

        repository = new ResourceHistoryRepository();
        repository.setOutputDir(csvDir.toString());
        repository.setRetentionDays(30);
    }

    @Test
    public void testSave() throws IOException {
        // Act
        MetricsJson metrics = new MetricsJson(
                UUID.randomUUID(),
                NoticeType.METRICS,
                ZonedDateTime.now(),
                "1.0.0",
                "test-host",
                new Metrics(75.0, 60.0, InstanceTypeChangeRequest.WITHIN));
        repository.save(metrics);

        // Assert
        assertTrue(Files.exists(csvFilePath));
        List<String> lines = Files.readAllLines(csvFilePath);
        assertTrue(lines.size() >= 2);
        assertEquals("Hostname,Timestamp,CpuUsage,MemoryUsage,InstanceTypeChangeRequest", lines.get(0));
        assertTrue(lines.get(1).startsWith("test-host,"));
    }

}
