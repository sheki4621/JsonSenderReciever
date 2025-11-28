package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.*;
import com.example.jsoncommon.dto.*;
import com.example.jsonreceiver.repository.*;
import com.example.jsonreceiver.util.ShellExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
public class InstanceStatusServiceTest {

        @Mock
        private InstanceStatusRepository repository;

        @Mock
        private AllInstanceRepository allInstanceRepository;

        @Mock
        private InstanceTypeLinkRepository instanceTypeLinkRepository;

        @Mock
        private InstanceTypeRepository instanceTypeRepository;

        @Mock
        private ShellExecutor shellExecutor;

        private InstanceStatusService service;

        @BeforeEach
        public void setUp() throws Exception {
                MockitoAnnotations.openMocks(this);
                service = new InstanceStatusService(
                                repository,
                                allInstanceRepository,
                                instanceTypeLinkRepository,
                                instanceTypeRepository,
                                shellExecutor);

                // シェルパスとタイムアウトを設定
                ReflectionTestUtils.setField(service, "installAgentShellPath", "/path/to/install_agent.sh");
                ReflectionTestUtils.setField(service, "uninstallAgentShellPath", "/path/to/uninstall_agent.sh");
                ReflectionTestUtils.setField(service, "shellTimeoutSeconds", 30);

                // デフォルトでSystemInfo、InstanceTypeLink、InstanceTypeが存在しないようにモック
                when(allInstanceRepository.findByHostname(anyString())).thenReturn(Optional.empty());
                when(instanceTypeLinkRepository.findByElType(anyString())).thenReturn(Optional.empty());
                when(instanceTypeRepository.findByInstanceTypeId(anyString())).thenReturn(Optional.empty());

                // シェル実行をデフォルトで成功するようにモック
                when(shellExecutor.executeShell(anyString(), anyList(), anyInt()))
                                .thenReturn("Shell execution successful");
        }

        @Test
        public void testProcessInstall() throws IOException {
                // Arrange
                InstallJson installJson = new InstallJson(UUID.randomUUID(),
                                ZonedDateTime.now(),
                                "1.0.0",
                                "test-host");

                when(repository.findByHostname("test-host")).thenReturn(Optional.empty());

                // Act
                service.processInstall(installJson);

                // Assert
                ArgumentCaptor<InstanceStatus> captor = ArgumentCaptor.forClass(InstanceStatus.class);
                verify(repository).save(captor.capture());

                InstanceStatus savedStatus = captor.getValue();
                assertEquals("test-host", savedStatus.getHostname());
                assertEquals(InstanceStatusValue.INSTALLING, savedStatus.getAgentStatus());
                assertEquals("1.0.0", savedStatus.getAgentVersion());
                assertNotNull(savedStatus.getLastUpdate());
        }

        @Test
        public void testProcessInstallWithSystemInfo() throws IOException {
                // Arrange
                InstallJson installJson = new InstallJson(UUID.randomUUID(),
                                ZonedDateTime.now(),
                                "1.0.0",
                                "test-host");

                // AllInstanceをモック
                AllInstance allInstance = new AllInstance("test-host", "ECS", "GROUP-A");
                when(allInstanceRepository.findByHostname("test-host")).thenReturn(Optional.of(allInstance));

                // InstanceTypeLinkをモック
                InstanceTypeLink link = new InstanceTypeLink("ECS", "1");
                when(instanceTypeLinkRepository.findByElType("ECS")).thenReturn(Optional.of(link));

                // InstanceTypeInfoをモック
                InstanceTypeInfo typeInfo = new InstanceTypeInfo("1", "c6i.8xlarge", 8, "c6i.2xlarge", 2, "c6i.micro",
                                1);
                when(instanceTypeRepository.findByInstanceTypeId("1")).thenReturn(Optional.of(typeInfo));

                when(repository.findByHostname("test-host")).thenReturn(Optional.empty());

                // Act
                service.processInstall(installJson);

                // Assert
                ArgumentCaptor<InstanceStatus> captor = ArgumentCaptor.forClass(InstanceStatus.class);
                verify(repository).save(captor.capture());

                InstanceStatus savedStatus = captor.getValue();
                assertEquals("test-host", savedStatus.getHostname());
                assertEquals("ECS", savedStatus.getMachineType());
                assertEquals("1", savedStatus.getTypeId());
                assertEquals("c6i.8xlarge", savedStatus.getTypeHigh());
                assertEquals("c6i.2xlarge", savedStatus.getTypeSmallStandard());
                assertEquals("c6i.micro", savedStatus.getTypeMicro());
                assertEquals(InstanceStatusValue.INSTALLING, savedStatus.getAgentStatus());
        }

        @Test
        public void testProcessUninstall() throws IOException {
                // Arrange
                UninstallJson uninstallJson = new UninstallJson(
                                UUID.randomUUID(),
                                ZonedDateTime.now(),
                                "1.0.0",
                                "test-host");

                when(repository.findByHostname("test-host")).thenReturn(Optional.empty());

                // Act
                service.processUninstall(uninstallJson);

                // Assert
                ArgumentCaptor<InstanceStatus> captor = ArgumentCaptor.forClass(InstanceStatus.class);
                verify(repository).save(captor.capture());

                InstanceStatus savedStatus = captor.getValue();
                assertEquals("test-host", savedStatus.getHostname());
                assertEquals(InstanceStatusValue.UNINSTALLING, savedStatus.getAgentStatus());
                assertEquals("1.0.0", savedStatus.getAgentVersion());
                assertNotNull(savedStatus.getLastUpdate());
        }

        @Test
        public void testProcessUp() throws IOException {
                // Arrange
                UpJson upJson = new UpJson(UUID.randomUUID(),
                                ZonedDateTime.now(),
                                "1.0.0",
                                "test-host");

                when(repository.findByHostname("test-host")).thenReturn(Optional.empty());

                // Act
                service.processUp(upJson);

                // Assert
                ArgumentCaptor<InstanceStatus> captor = ArgumentCaptor.forClass(InstanceStatus.class);
                verify(repository).save(captor.capture());

                InstanceStatus savedStatus = captor.getValue();
                assertEquals("test-host", savedStatus.getHostname());
                assertEquals(InstanceStatusValue.UP, savedStatus.getAgentStatus());
                assertEquals("1.0.0", savedStatus.getAgentVersion());
                assertNotNull(savedStatus.getLastUpdate());
        }

        @Test
        public void testProcessUpWithExistingData() throws IOException {
                // Arrange
                UpJson upJson = new UpJson(UUID.randomUUID(),
                                ZonedDateTime.now(),
                                "1.1.0",
                                "test-host");

                // 既存データを返す
                InstanceStatus existingStatus = new InstanceStatus(
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
                                "1.0.0",
                                "");
                when(repository.findByHostname("test-host")).thenReturn(Optional.of(existingStatus));

                // Act
                service.processUp(upJson);

                // Assert
                ArgumentCaptor<InstanceStatus> captor = ArgumentCaptor.forClass(InstanceStatus.class);
                verify(repository).save(captor.capture());

                InstanceStatus savedStatus = captor.getValue();
                assertEquals("test-host", savedStatus.getHostname());
                assertEquals("ECS", savedStatus.getMachineType());
                assertEquals("c6i.2xlarge", savedStatus.getCurrentType());
                assertEquals(InstanceStatusValue.UP, savedStatus.getAgentStatus());
                assertEquals("1.1.0", savedStatus.getAgentVersion());
        }

        @Test
        public void testProcessDown() throws IOException {
                // Arrange
                DownJson downJson = new DownJson(
                                UUID.randomUUID(),
                                ZonedDateTime.now(),
                                "1.0.0",
                                "test-host");

                when(repository.findByHostname("test-host")).thenReturn(Optional.empty());

                // Act
                service.processDown(downJson);

                // Assert
                ArgumentCaptor<InstanceStatus> captor = ArgumentCaptor.forClass(InstanceStatus.class);
                verify(repository).save(captor.capture());

                InstanceStatus savedStatus = captor.getValue();
                assertEquals("test-host", savedStatus.getHostname());
                assertEquals(InstanceStatusValue.DOWN, savedStatus.getAgentStatus());
                assertEquals("1.0.0", savedStatus.getAgentVersion());
                assertNotNull(savedStatus.getLastUpdate());
        }
}
