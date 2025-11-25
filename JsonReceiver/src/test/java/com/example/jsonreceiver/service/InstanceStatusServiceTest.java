package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.*;
import com.example.jsonreceiver.repository.InstanceStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class InstanceStatusServiceTest {

    @Mock
    private InstanceStatusRepository repository;

    private InstanceStatusService service;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new InstanceStatusService(repository);
    }

    @Test
    public void testProcessInstall() throws IOException {
        // Arrange
        InstallJson installJson = new InstallJson(
                UUID.randomUUID(),
                NoticeType.INSTALL,
                ZonedDateTime.now(),
                "1.0.0",
                "test-host");

        // Act
        service.processInstall(installJson);

        // Assert
        ArgumentCaptor<InstanceStatus> captor = ArgumentCaptor.forClass(InstanceStatus.class);
        verify(repository).save(captor.capture());

        InstanceStatus savedStatus = captor.getValue();
        assertEquals("test-host", savedStatus.getHostname());
        assertEquals(InstanceStatusValue.INSTALLING, savedStatus.getStatus());
        assertFalse(savedStatus.getIsInstalled());
        assertEquals("1.0.0", savedStatus.getAgentVersion());
        assertNotNull(savedStatus.getTimestamp());
    }

    @Test
    public void testProcessUninstall() throws IOException {
        // Arrange
        UninstallJson uninstallJson = new UninstallJson(
                UUID.randomUUID(),
                NoticeType.UNINSTALL,
                ZonedDateTime.now(),
                "1.0.0",
                "test-host");

        // Act
        service.processUninstall(uninstallJson);

        // Assert
        ArgumentCaptor<InstanceStatus> captor = ArgumentCaptor.forClass(InstanceStatus.class);
        verify(repository).save(captor.capture());

        InstanceStatus savedStatus = captor.getValue();
        assertEquals("test-host", savedStatus.getHostname());
        assertEquals(InstanceStatusValue.UNINSTALLING, savedStatus.getStatus());
        assertFalse(savedStatus.getIsInstalled());
        assertEquals("1.0.0", savedStatus.getAgentVersion());
        assertNotNull(savedStatus.getTimestamp());
    }

    @Test
    public void testProcessUpFromInstalling() throws IOException {
        // Arrange
        UpJson upJson = new UpJson(
                UUID.randomUUID(),
                NoticeType.UP,
                ZonedDateTime.now(),
                "1.1.0",
                "test-host");

        // 既存のINSTALLING状態を返す
        InstanceStatus existingStatus = new InstanceStatus(
                "test-host",
                InstanceStatusValue.INSTALLING,
                false,
                "1.0.0",
                ZonedDateTime.now().toString());
        when(repository.findByHostname("test-host")).thenReturn(Optional.of(existingStatus));

        // Act
        service.processUp(upJson);

        // Assert
        ArgumentCaptor<InstanceStatus> captor = ArgumentCaptor.forClass(InstanceStatus.class);
        verify(repository).save(captor.capture());

        InstanceStatus savedStatus = captor.getValue();
        assertEquals("test-host", savedStatus.getHostname());
        assertEquals(InstanceStatusValue.UP, savedStatus.getStatus());
        assertTrue(savedStatus.getIsInstalled()); // INSTALLINGからUPなのでtrueに
        assertEquals("1.1.0", savedStatus.getAgentVersion());
        assertNotNull(savedStatus.getTimestamp());
    }

    @Test
    public void testProcessUpFromOtherStatus() throws IOException {
        // Arrange
        UpJson upJson = new UpJson(
                UUID.randomUUID(),
                NoticeType.UP,
                ZonedDateTime.now(),
                "1.1.0",
                "test-host");

        // 既存のDOWN状態を返す
        InstanceStatus existingStatus = new InstanceStatus(
                "test-host",
                InstanceStatusValue.DOWN,
                true,
                "1.0.0",
                ZonedDateTime.now().toString());
        when(repository.findByHostname("test-host")).thenReturn(Optional.of(existingStatus));

        // Act
        service.processUp(upJson);

        // Assert
        ArgumentCaptor<InstanceStatus> captor = ArgumentCaptor.forClass(InstanceStatus.class);
        verify(repository).save(captor.capture());

        InstanceStatus savedStatus = captor.getValue();
        assertEquals("test-host", savedStatus.getHostname());
        assertEquals(InstanceStatusValue.UP, savedStatus.getStatus());
        assertTrue(savedStatus.getIsInstalled()); // 既存のIsInstalled値を保持
        assertEquals("1.1.0", savedStatus.getAgentVersion());
        assertNotNull(savedStatus.getTimestamp());
    }

    @Test
    public void testProcessUpNewHost() throws IOException {
        // Arrange
        UpJson upJson = new UpJson(
                UUID.randomUUID(),
                NoticeType.UP,
                ZonedDateTime.now(),
                "1.0.0",
                "new-host");

        // 新規ホスト（既存データなし）
        when(repository.findByHostname("new-host")).thenReturn(Optional.empty());

        // Act
        service.processUp(upJson);

        // Assert
        ArgumentCaptor<InstanceStatus> captor = ArgumentCaptor.forClass(InstanceStatus.class);
        verify(repository).save(captor.capture());

        InstanceStatus savedStatus = captor.getValue();
        assertEquals("new-host", savedStatus.getHostname());
        assertEquals(InstanceStatusValue.UP, savedStatus.getStatus());
        assertTrue(savedStatus.getIsInstalled()); // 新規の場合はtrueに
        assertEquals("1.0.0", savedStatus.getAgentVersion());
        assertNotNull(savedStatus.getTimestamp());
    }

    @Test
    public void testProcessDownFromUninstalling() throws IOException {
        // Arrange
        DownJson downJson = new DownJson(
                UUID.randomUUID(),
                NoticeType.DOWN,
                ZonedDateTime.now(),
                "1.0.0",
                "test-host");

        // 既存のUNINSTALLING状態を返す
        InstanceStatus existingStatus = new InstanceStatus(
                "test-host",
                InstanceStatusValue.UNINSTALLING,
                true,
                "1.0.0",
                ZonedDateTime.now().toString());
        when(repository.findByHostname("test-host")).thenReturn(Optional.of(existingStatus));

        // Act
        service.processDown(downJson);

        // Assert
        ArgumentCaptor<InstanceStatus> captor = ArgumentCaptor.forClass(InstanceStatus.class);
        verify(repository).save(captor.capture());

        InstanceStatus savedStatus = captor.getValue();
        assertEquals("test-host", savedStatus.getHostname());
        assertEquals(InstanceStatusValue.DOWN, savedStatus.getStatus());
        assertFalse(savedStatus.getIsInstalled()); // UNINSTALLINGからDOWNなのでfalseに
        assertEquals("1.0.0", savedStatus.getAgentVersion());
        assertNotNull(savedStatus.getTimestamp());
    }

    @Test
    public void testProcessDownFromOtherStatus() throws IOException {
        // Arrange
        DownJson downJson = new DownJson(
                UUID.randomUUID(),
                NoticeType.DOWN,
                ZonedDateTime.now(),
                "1.0.0",
                "test-host");

        // 既存のUP状態を返す
        InstanceStatus existingStatus = new InstanceStatus(
                "test-host",
                InstanceStatusValue.UP,
                true,
                "1.0.0",
                ZonedDateTime.now().toString());
        when(repository.findByHostname("test-host")).thenReturn(Optional.of(existingStatus));

        // Act
        service.processDown(downJson);

        // Assert
        ArgumentCaptor<InstanceStatus> captor = ArgumentCaptor.forClass(InstanceStatus.class);
        verify(repository).save(captor.capture());

        InstanceStatus savedStatus = captor.getValue();
        assertEquals("test-host", savedStatus.getHostname());
        assertEquals(InstanceStatusValue.DOWN, savedStatus.getStatus());
        assertTrue(savedStatus.getIsInstalled()); // 既存のIsInstalled値を保持
        assertEquals("1.0.0", savedStatus.getAgentVersion());
        assertNotNull(savedStatus.getTimestamp());
    }
}
