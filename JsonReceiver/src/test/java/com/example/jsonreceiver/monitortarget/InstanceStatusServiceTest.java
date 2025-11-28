package com.example.jsonreceiver.monitortarget;

import com.example.jsoncommon.dto.UpJson;
import com.example.jsoncommon.dto.DownJson;
import com.example.jsonreceiver.instancetype.*;
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

                // デフォルトでSystemInfo、InstanceTypeLinkCsv、InstanceTypeが存在しないようにモック
                when(allInstanceRepository.findByHostname(anyString())).thenReturn(Optional.empty());
                when(instanceTypeLinkRepository.findByElType(anyString())).thenReturn(Optional.empty());
                when(instanceTypeRepository.findByInstanceTypeId(anyString())).thenReturn(Optional.empty());

                // シェル実行をデフォルトで成功するようにモック
                when(shellExecutor.executeShell(anyString(), anyList(), anyInt()))
                                .thenReturn("Shell execution successful");
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
                ArgumentCaptor<InstanceStatusCsv> captor = ArgumentCaptor.forClass(InstanceStatusCsv.class);
                verify(repository).save(captor.capture());

                InstanceStatusCsv savedStatus = captor.getValue();
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
                InstanceStatusCsv existingStatus = new InstanceStatusCsv(
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
                ArgumentCaptor<InstanceStatusCsv> captor = ArgumentCaptor.forClass(InstanceStatusCsv.class);
                verify(repository).save(captor.capture());

                InstanceStatusCsv savedStatus = captor.getValue();
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
                ArgumentCaptor<InstanceStatusCsv> captor = ArgumentCaptor.forClass(InstanceStatusCsv.class);
                verify(repository).save(captor.capture());

                InstanceStatusCsv savedStatus = captor.getValue();
                assertEquals("test-host", savedStatus.getHostname());
                assertEquals(InstanceStatusValue.DOWN, savedStatus.getAgentStatus());
                assertEquals("1.0.0", savedStatus.getAgentVersion());
                assertNotNull(savedStatus.getLastUpdate());
        }
}
