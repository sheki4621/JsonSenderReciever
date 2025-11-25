package com.example.jsonreceiver.repository;

import com.example.jsonreceiver.dto.InstanceStatus;
import com.example.jsonreceiver.dto.InstanceStatusValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class InstanceStatusRepositoryTest {

    @TempDir
    Path tempDir;

    private InstanceStatusRepository repository;
    private Path csvFilePath;

    @BeforeEach
    public void setUp() {
        Path csvDir = tempDir.resolve("csv");
        try {
            Files.createDirectories(csvDir);
        } catch (IOException e) {
            fail("Failed to create test directory");
        }
        csvFilePath = csvDir.resolve("InstanceStatus.csv");

        // Repositoryを作成し、outputDirをテスト用に設定
        repository = new InstanceStatusRepository();
        repository.setOutputDir(csvDir.toString());
    }

    @Test
    public void testSaveInstanceStatus() throws IOException {
        // Arrange
        InstanceStatus status = new InstanceStatus(
                "test-host",
                InstanceStatusValue.UP,
                true,
                "1.0.0",
                ZonedDateTime.now().toString());

        // Act
        repository.save(status);

        // Assert
        assertTrue(Files.exists(csvFilePath), "CSV file should exist");
        List<String> lines = Files.readAllLines(csvFilePath);
        assertTrue(lines.size() >= 2, "CSV should have header and at least one data line");
        assertEquals("Hostname,Status,IsInstalled,AgentVersion,Timestamp", lines.get(0));
        assertTrue(lines.get(1).startsWith("test-host,UP,true,1.0.0,"));
    }

    @Test
    public void testFindByHostname() throws IOException {
        // Arrange
        InstanceStatus status1 = new InstanceStatus(
                "host1",
                InstanceStatusValue.UP,
                true,
                "1.0.0",
                ZonedDateTime.now().toString());
        InstanceStatus status2 = new InstanceStatus(
                "host2",
                InstanceStatusValue.DOWN,
                false,
                "1.1.0",
                ZonedDateTime.now().toString());
        repository.save(status1);
        repository.save(status2);

        // Act
        Optional<InstanceStatus> found = repository.findByHostname("host1");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("host1", found.get().getHostname());
        assertEquals(InstanceStatusValue.UP, found.get().getStatus());
        assertTrue(found.get().getIsInstalled());
        assertEquals("1.0.0", found.get().getAgentVersion());
    }

    @Test
    public void testFindByHostnameNotFound() throws IOException {
        // Act
        Optional<InstanceStatus> found = repository.findByHostname("non-existent");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    public void testUpdateExistingInstance() throws IOException {
        // Arrange
        InstanceStatus initialStatus = new InstanceStatus(
                "test-host",
                InstanceStatusValue.INSTALLING,
                false,
                "1.0.0",
                ZonedDateTime.now().toString());
        repository.save(initialStatus);

        // Act - Update the same host
        InstanceStatus updatedStatus = new InstanceStatus(
                "test-host",
                InstanceStatusValue.UP,
                true,
                "1.1.0",
                ZonedDateTime.now().toString());
        repository.save(updatedStatus);

        // Assert
        Optional<InstanceStatus> found = repository.findByHostname("test-host");
        assertTrue(found.isPresent());
        assertEquals(InstanceStatusValue.UP, found.get().getStatus());
        assertTrue(found.get().getIsInstalled());
        assertEquals("1.1.0", found.get().getAgentVersion());
    }
}
