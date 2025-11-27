package com.example.jsonreceiver.repository;

import com.example.jsonreceiver.dto.InstanceStatus;
import com.example.jsonreceiver.dto.InstanceStatusValue;
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
        public void setUp() throws IOException {
                repository = new InstanceStatusRepository();
                repository.setOutputDir(tempDir.toString());
                csvFilePath = tempDir.resolve("monitor_target.csv");
        }

        @Test
        public void testSaveInstanceStatus() throws IOException {
                // Arrange
                InstanceStatus status = new InstanceStatus(
                                "test-host",
                                "ECS",
                                "ap-northeast-1",
                                "c6i.4xlarge",
                                "1",
                                "c6i.8xlarge",
                                "c6i.2xlarge",
                                "c6i.micro",
                                ZonedDateTime.now().toString(),
                                InstanceStatusValue.UP,
                                "1.0.0");

                // Act
                repository.save(status);

                // Assert
                assertTrue(Files.exists(csvFilePath), "CSV file should exist");
                List<String> lines = Files.readAllLines(csvFilePath);
                assertTrue(lines.size() >= 2, "CSV should have header and at least one data line");
                assertEquals(
                                "HOSTNAME,MACHINE_TYPE,REGION,CURRENT_TYPE,TYPE_ID,TYPE_HIGH,TYPE_SMALL_STANDARD,TYPE_MICRO,LASTUPDATE,AGENT_STATUS,AGENT_VERSION",
                                lines.get(0));
                assertTrue(lines.get(1)
                                .startsWith("test-host,ECS,ap-northeast-1,c6i.4xlarge,1,c6i.8xlarge,c6i.2xlarge,c6i.micro,"));
        }

        @Test
        public void testFindByHostname() throws IOException {
                // Arrange
                InstanceStatus status1 = new InstanceStatus(
                                "host1",
                                "ECS",
                                "us-east-1",
                                "t2.large",
                                "1",
                                "t2.xlarge",
                                "t2.medium",
                                "t2.micro",
                                ZonedDateTime.now().toString(),
                                InstanceStatusValue.UP,
                                "1.0.0");
                InstanceStatus status2 = new InstanceStatus(
                                "host2",
                                "EDB",
                                "us-west-2",
                                "t3.large",
                                "2",
                                "t3.xlarge",
                                "t3.medium",
                                "t3.micro",
                                ZonedDateTime.now().toString(),
                                InstanceStatusValue.DOWN,
                                "1.1.0");
                repository.save(status1);
                repository.save(status2);

                // Act
                Optional<InstanceStatus> found = repository.findByHostname("host1");

                // Assert
                assertTrue(found.isPresent());
                assertEquals("host1", found.get().getHostname());
                assertEquals(InstanceStatusValue.UP, found.get().getAgentStatus());
                assertEquals("1.0.0", found.get().getAgentVersion());
                assertEquals("ECS", found.get().getMachineType());
                assertEquals("t2.large", found.get().getCurrentType());
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
                                "ECS",
                                "ap-northeast-1",
                                "c6i.2xlarge",
                                "1",
                                "c6i.8xlarge",
                                "c6i.2xlarge",
                                "c6i.micro",
                                ZonedDateTime.now().toString(),
                                InstanceStatusValue.INSTALLING,
                                "1.0.0");
                repository.save(initialStatus);

                // Act - Update the same host
                InstanceStatus updatedStatus = new InstanceStatus(
                                "test-host",
                                "ECS",
                                "ap-northeast-1",
                                "c6i.8xlarge",
                                "1",
                                "c6i.8xlarge",
                                "c6i.2xlarge",
                                "c6i.micro",
                                ZonedDateTime.now().toString(),
                                InstanceStatusValue.UP,
                                "1.1.0");
                repository.save(updatedStatus);

                // Assert
                Optional<InstanceStatus> found = repository.findByHostname("test-host");
                assertTrue(found.isPresent());
                assertEquals(InstanceStatusValue.UP, found.get().getAgentStatus());
                assertEquals("1.1.0", found.get().getAgentVersion());
                assertEquals("c6i.8xlarge", found.get().getCurrentType());
        }

        @Test
        public void testUpdateCurrentType() throws IOException {
                // Arrange
                InstanceStatus initialStatus = new InstanceStatus(
                                "test-host",
                                "ECS",
                                "ap-northeast-1",
                                "c6i.2xlarge",
                                "1",
                                "c6i.8xlarge",
                                "c6i.2xlarge",
                                "c6i.micro",
                                ZonedDateTime.now().toString(),
                                InstanceStatusValue.UP,
                                "1.0.0");
                repository.save(initialStatus);

                // Act
                repository.updateCurrentType("test-host", "c6i.8xlarge");

                // Assert
                Optional<InstanceStatus> found = repository.findByHostname("test-host");
                assertTrue(found.isPresent());
                assertEquals("c6i.8xlarge", found.get().getCurrentType());
                assertEquals(InstanceStatusValue.UP, found.get().getAgentStatus());
        }
}
