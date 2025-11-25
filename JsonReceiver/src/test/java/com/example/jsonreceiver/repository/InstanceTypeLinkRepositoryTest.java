package com.example.jsonreceiver.repository;

import com.example.jsonreceiver.dto.InstanceTypeLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class InstanceTypeLinkRepositoryTest {

    @TempDir
    Path tempDir;

    private InstanceTypeLinkRepository repository;
    private Path csvFilePath;

    @BeforeEach
    public void setUp() throws IOException {
        Path csvDir = tempDir.resolve("csv");
        Files.createDirectories(csvDir);
        csvFilePath = csvDir.resolve("InstanceTypeLink.csv");

        // テスト用のCSVデータを作成
        String csvContent = """
                ElType,InstanceTypeId
                t2.micro,1
                t2.small,2
                t2.medium,3
                """;
        Files.writeString(csvFilePath, csvContent);

        repository = new InstanceTypeLinkRepository();
        repository.setOutputDir(csvDir.toString());
    }

    @Test
    public void testFindByElType_Found() throws IOException {
        // Act
        Optional<InstanceTypeLink> result = repository.findByElType("t2.micro");

        // Assert
        assertTrue(result.isPresent());
        InstanceTypeLink link = result.get();
        assertEquals("t2.micro", link.getElType());
        assertEquals("1", link.getInstanceTypeId());
    }

    @Test
    public void testFindByElType_NotFound() throws IOException {
        // Act
        Optional<InstanceTypeLink> result = repository.findByElType("non-existent");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindByElType_MultipleRecords() throws IOException {
        // Act
        Optional<InstanceTypeLink> result = repository.findByElType("t2.medium");

        // Assert
        assertTrue(result.isPresent());
        InstanceTypeLink link = result.get();
        assertEquals("t2.medium", link.getElType());
        assertEquals("3", link.getInstanceTypeId());
    }

    @Test
    public void testFindByElType_FileNotExists() throws IOException {
        // Arrange - ファイルを削除
        Files.delete(csvFilePath);

        // Act
        Optional<InstanceTypeLink> result = repository.findByElType("t2.micro");

        // Assert
        assertFalse(result.isPresent());
    }
}
