package com.example.jsonreceiver.repository;

import com.example.jsoncommon.dto.InstanceTypeChangeRequest;
import com.example.jsoncommon.dto.Metrics;
import com.example.jsoncommon.dto.MetricsJson;
import com.example.jsoncommon.dto.NoticeType;
import com.example.jsoncommon.dto.ResourceHistoryCsv;
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

    @Test
    public void testFindLastNByHostname_SingleRecord() throws IOException {
        // Arrange
        repository.save(new MetricsJson(
                UUID.randomUUID(),
                NoticeType.METRICS,
                ZonedDateTime.now(),
                "1.0.0",
                "host1",
                new Metrics(50.0, 60.0, InstanceTypeChangeRequest.WITHIN)));

        // Act
        List<ResourceHistoryCsv> result = repository.findLastNByHostname("host1", 5);

        // Assert
        assertEquals(1, result.size());
        assertEquals("host1", result.get(0).getHostname());
        assertEquals(50.0, result.get(0).getCpuUsage());
        assertEquals(60.0, result.get(0).getMemoryUsage());
    }

    @Test
    public void testFindLastNByHostname_MultipleRecords() throws IOException {
        // Arrange - 5件のレコードを追加
        // Arrange - 5件のレコードを追加
        // 期待値: 最新から 55.0, 54.0, 53.0 ...
        // 保存順: 1 -> 5 (最新)
        // i=1: 51.0
        // i=2: 52.0
        // i=3: 53.0
        // i=4: 54.0
        // i=5: 55.0
        for (int i = 1; i <= 5; i++) {
            repository.save(new MetricsJson(
                    UUID.randomUUID(),
                    NoticeType.METRICS,
                    ZonedDateTime.now().plusMinutes(i), // タイムスタンプもずらす
                    "1.0.0",
                    "host1",
                    new Metrics(50.0 + i, 60.0, InstanceTypeChangeRequest.WITHIN)));
        }
        // 別のホストのレコードも追加
        repository.save(new MetricsJson(
                UUID.randomUUID(),
                NoticeType.METRICS,
                ZonedDateTime.now(),
                "1.0.0",
                "host2",
                new Metrics(75.0, 60.0, InstanceTypeChangeRequest.WITHIN)));

        // Act - 最新3件を取得
        List<ResourceHistoryCsv> result = repository.findLastNByHostname("host1", 3);

        // Assert
        assertEquals(3, result.size());
        // 最新のものから順に取得されることを確認
        assertEquals(55.0, result.get(0).getCpuUsage());
        assertEquals(54.0, result.get(1).getCpuUsage());
        assertEquals(53.0, result.get(2).getCpuUsage());
    }

    @Test
    public void testFindLastNByHostname_LessThanN() throws IOException {
        // Arrange - 2件のレコードを追加
        repository.save(new MetricsJson(
                UUID.randomUUID(),
                NoticeType.METRICS,
                ZonedDateTime.now(),
                "1.0.0",
                "host1",
                new Metrics(75.0, 60.0, InstanceTypeChangeRequest.WITHIN)));
        repository.save(new MetricsJson(
                UUID.randomUUID(),
                NoticeType.METRICS,
                ZonedDateTime.now(),
                "1.0.0",
                "host1",
                new Metrics(75.0, 60.0, InstanceTypeChangeRequest.WITHIN)));

        // Act - 5件要求するが2件しかない
        List<ResourceHistoryCsv> result = repository.findLastNByHostname("host1", 5);

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    public void testFindLastNByHostname_NotFound() throws IOException {
        // Arrange
        repository.save(new MetricsJson(
                UUID.randomUUID(),
                NoticeType.METRICS,
                ZonedDateTime.now(),
                "1.0.0",
                "host1",
                new Metrics(75.0, 60.0, InstanceTypeChangeRequest.WITHIN)));

        // Act
        List<ResourceHistoryCsv> result = repository.findLastNByHostname("non-existent", 5);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void testFindLastNByHostname_FileNotExists() throws IOException {
        // Act
        List<ResourceHistoryCsv> result = repository.findLastNByHostname("host1", 5);

        // Assert
        assertEquals(0, result.size());
    }
}
