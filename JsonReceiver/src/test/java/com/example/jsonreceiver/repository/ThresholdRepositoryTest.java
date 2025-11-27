package com.example.jsonreceiver.repository;

import com.example.jsoncommon.dto.ThresholdInfo;
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
        repository = new ThresholdRepository();
        repository.setOutputDir(csvDir.toString());
        csvFilePath = csvDir.resolve(repository.getFilePath());

        // テスト用のCSVデータを作成
        ThresholdInfo info1 = new ThresholdInfo();
        info1.setHostname("test-host1");
        info1.setUpperCpuThreshold(80.0);
        info1.setLowerCpuThreshold(20.0);
        info1.setUpperMemThreshold(80.0);
        info1.setLowerMemThreshold(20.0);
        info1.setUpperCpuDurationMin(5);
        repository.save(info1);

        ThresholdInfo info2 = new ThresholdInfo();
        info2.setHostname("test-host2");
        info2.setUpperCpuThreshold(90.0);
        info2.setLowerCpuThreshold(30.0);
        info2.setUpperMemThreshold(90.0);
        info2.setLowerMemThreshold(30.0);
        info2.setUpperCpuDurationMin(5);
        repository.save(info2);

        ThresholdInfo info3 = new ThresholdInfo();
        info3.setHostname("test-host3");
        info3.setUpperCpuThreshold(75.0);
        info3.setLowerCpuThreshold(15.0);
        info3.setUpperMemThreshold(80.0);
        info3.setLowerMemThreshold(20.0);
        info3.setUpperCpuDurationMin(2);
        repository.save(info3);
    }

    @Test
    public void testFindByHostname_Found() throws IOException {
        // Act
        Optional<ThresholdInfo> result = repository.findByHostname("test-host1");

        // Assert
        assertTrue(result.isPresent());
        ThresholdInfo threshold = result.get();
        assertEquals("test-host1", threshold.getHostname());
        assertEquals(80.0, result.get().getUpperCpuThreshold());
        assertEquals(20.0, result.get().getLowerCpuThreshold());
        assertEquals(80.0, result.get().getUpperMemThreshold());
        assertEquals(20.0, result.get().getLowerMemThreshold());
        assertEquals(5, result.get().getUpperCpuDurationMin());
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
        assertEquals(90.0, result.get().getUpperCpuThreshold());
        assertEquals(30.0, result.get().getLowerCpuThreshold());
        assertEquals(90.0, result.get().getUpperMemThreshold());
        assertEquals(30.0, result.get().getLowerMemThreshold());
        assertEquals(5, result.get().getUpperCpuDurationMin());
    }

    @Test
    public void testFindByHostname_FileNotExists() throws IOException {
        // Arrange - ファイルを削除
        Files.delete(csvFilePath);

        // Act
        Optional<ThresholdInfo> result;
        try {
            result = repository.findByHostname("test-host1");
        } catch (java.nio.file.NoSuchFileException e) {
            result = Optional.empty();
        }

        // Assert
        assertFalse(result.isPresent());
    }
}
