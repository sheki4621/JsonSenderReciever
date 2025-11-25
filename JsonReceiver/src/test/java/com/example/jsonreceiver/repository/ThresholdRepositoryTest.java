package com.example.jsonreceiver.repository;

import com.example.jsonreceiver.dto.ThresholdInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ThresholdRepositoryTest {

    @TempDir
    Path tempDir;

    private ThresholdRepository repository;
    private Path csvFilePath;

    @BeforeEach
    public void setUp() throws IOException {
        Path csvDir = tempDir.resolve("csv");
        Files.createDirectories(csvDir);
        csvFilePath = csvDir.resolve("Threshold.csv");

        // テスト用のCSVデータを作成
        String csvContent = """
                Hostname,CpuUpperLimit,CpuLowerLimit,MemoryUpperLimit,MemoryLowerLimit,ContinueCount
                test-host1,80.0,20.0,85.0,25.0,3
                test-host2,90.0,30.0,90.0,30.0,5
                test-host3,75.0,15.0,80.0,20.0,2
                """;
        Files.writeString(csvFilePath, csvContent);

        repository = new ThresholdRepository();
        repository.setOutputDir(csvDir.toString());
    }

    @Test
    public void testFindByHostname_Found() throws IOException {
        // Act
        Optional<ThresholdInfo> result = repository.findByHostname("test-host1");

        // Assert
        assertTrue(result.isPresent());
        ThresholdInfo threshold = result.get();
        assertEquals("test-host1", threshold.getHostname());
        assertEquals(80.0, threshold.getCpuUpperLimit());
        assertEquals(20.0, threshold.getCpuLowerLimit());
        assertEquals(85.0, threshold.getMemoryUpperLimit());
        assertEquals(25.0, threshold.getMemoryLowerLimit());
        assertEquals(3, threshold.getContinueCount());
    }

    @Test
    public void testFindByHostname_NotFound() throws IOException {
        // Act
        Optional<ThresholdInfo> result = repository.findByHostname("non-existent");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindByHostname_MultipleRecords() throws IOException {
        // Act
        Optional<ThresholdInfo> result = repository.findByHostname("test-host2");

        // Assert
        assertTrue(result.isPresent());
        ThresholdInfo threshold = result.get();
        assertEquals("test-host2", threshold.getHostname());
        assertEquals(90.0, threshold.getCpuUpperLimit());
        assertEquals(30.0, threshold.getCpuLowerLimit());
        assertEquals(90.0, threshold.getMemoryUpperLimit());
        assertEquals(30.0, threshold.getMemoryLowerLimit());
        assertEquals(5, threshold.getContinueCount());
    }

    @Test
    public void testFindByHostname_FileNotExists() throws IOException {
        // Arrange - ファイルを削除
        Files.delete(csvFilePath);

        // Act
        Optional<ThresholdInfo> result = repository.findByHostname("test-host1");

        // Assert
        assertFalse(result.isPresent());
    }
}
