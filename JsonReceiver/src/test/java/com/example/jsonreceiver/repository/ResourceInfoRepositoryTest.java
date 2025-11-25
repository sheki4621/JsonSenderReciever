package com.example.jsonreceiver.repository;

import com.example.jsonreceiver.dto.ResourceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceInfoRepositoryTest {

    @TempDir
    Path tempDir;

    private ResourceInfoRepository repository;
    private Path csvFilePath;

    @BeforeEach
    public void setUp() throws IOException {
        Path csvDir = tempDir.resolve("csv");
        Files.createDirectories(csvDir);
        csvFilePath = csvDir.resolve("ResourceInfo.csv");

        repository = new ResourceInfoRepository();
        repository.setOutputDir(csvDir.toString());
    }

    @Test
    public void testSave() throws IOException {
        // Act
        repository.save("test-host", ZonedDateTime.now().toString(), "50.0", "60.0");

        // Assert
        assertTrue(Files.exists(csvFilePath));
        List<String> lines = Files.readAllLines(csvFilePath);
        assertTrue(lines.size() >= 2);
        assertEquals("Hostname,Timestamp,CpuUsage,MemoryUsage", lines.get(0));
        assertTrue(lines.get(1).startsWith("test-host,"));
    }

    @Test
    public void testFindLastNByHostname_SingleRecord() throws IOException {
        // Arrange
        repository.save("host1", "2025-11-26T00:00:00+09:00", "50.0", "60.0");

        // Act
        List<ResourceInfo> result = repository.findLastNByHostname("host1", 5);

        // Assert
        assertEquals(1, result.size());
        assertEquals("host1", result.get(0).getHostname());
        assertEquals(50.0, result.get(0).getCpuUsage());
        assertEquals(60.0, result.get(0).getMemoryUsage());
    }

    @Test
    public void testFindLastNByHostname_MultipleRecords() throws IOException {
        // Arrange - 5件のレコードを追加
        for (int i = 1; i <= 5; i++) {
            repository.save("host1", "2025-11-26T00:0" + i + ":00+09:00", String.valueOf(50.0 + i),
                    String.valueOf(60.0 + i));
        }
        // 別のホストのレコードも追加
        repository.save("host2", "2025-11-26T00:06:00+09:00", "70.0", "80.0");

        // Act - 最新3件を取得
        List<ResourceInfo> result = repository.findLastNByHostname("host1", 3);

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
        repository.save("host1", "2025-11-26T00:01:00+09:00", "50.0", "60.0");
        repository.save("host1", "2025-11-26T00:02:00+09:00", "51.0", "61.0");

        // Act - 5件要求するが2件しかない
        List<ResourceInfo> result = repository.findLastNByHostname("host1", 5);

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    public void testFindLastNByHostname_NotFound() throws IOException {
        // Arrange
        repository.save("host1", "2025-11-26T00:01:00+09:00", "50.0", "60.0");

        // Act
        List<ResourceInfo> result = repository.findLastNByHostname("non-existent", 5);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    public void testFindLastNByHostname_FileNotExists() throws IOException {
        // Act
        List<ResourceInfo> result = repository.findLastNByHostname("host1", 5);

        // Assert
        assertEquals(0, result.size());
    }
}
