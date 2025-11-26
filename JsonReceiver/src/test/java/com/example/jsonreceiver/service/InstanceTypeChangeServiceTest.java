package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.InstanceTypeInfo;
import com.example.jsonreceiver.dto.SystemInfo;
import com.example.jsonreceiver.dto.InstanceType;
import com.example.jsonreceiver.repository.InstanceStatusRepository;
import com.example.jsonreceiver.repository.InstanceTypeLinkRepository;
import com.example.jsonreceiver.repository.InstanceTypeRepository;
import com.example.jsonreceiver.repository.SystemInfoRepository;
import com.example.jsonreceiver.dto.InstanceTypeLink;
import com.example.jsonreceiver.util.ShellExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class InstanceTypeChangeServiceTest {

    @Mock
    private InstanceStatusRepository instanceStatusRepository;

    @Mock
    private SystemInfoRepository systemInfoRepository;

    @Mock
    private InstanceTypeLinkRepository instanceTypeLinkRepository;

    @Mock
    private InstanceTypeRepository instanceTypeRepository;

    @Mock
    private ShellExecutor shellExecutor;

    private InstanceTypeChangeService service;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        service = new InstanceTypeChangeService(
                instanceStatusRepository,
                systemInfoRepository,
                instanceTypeLinkRepository,
                instanceTypeRepository,
                shellExecutor);

        // 設定値を注入
        ReflectionTestUtils.setField(service, "executeInstanceTypeChangeShellPath", "/path/to/change.sh");
        ReflectionTestUtils.setField(service, "checkInstanceTypeChangeShellPath", "/path/to/check.sh");
        ReflectionTestUtils.setField(service, "shellTimeoutSeconds", 30);
        ReflectionTestUtils.setField(service, "checkIntervalSeconds", 5);
        ReflectionTestUtils.setField(service, "maxRetryCount", 3);

        // シェル実行をデフォルトで成功するようにモック
        // インスタンスタイプ変更実行用のシェル
        when(shellExecutor.executeShell(eq("/path/to/change.sh"), anyList(), anyInt()))
                .thenReturn("Success");

        // インスタンスタイプ変更完了確認用のシェル（"COMPLETED"を返す）
        when(shellExecutor.executeShell(eq("/path/to/check.sh"), anyList(), anyInt()))
                .thenReturn("COMPLETED");
    }

    @Test
    public void testChangeInstanceType_HIGH() throws IOException {
        // Arrange
        SystemInfo systemInfo = new SystemInfo("192.168.1.1", "test-host", "EL-A", "HEL-01");
        InstanceTypeLink link = new InstanceTypeLink("EL-A", "1");
        InstanceTypeInfo typeInfo = new InstanceTypeInfo("1", "t2.xlarge", 4, "t2.medium", 2, "t2.micro", 1);

        when(systemInfoRepository.findByHostname("test-host")).thenReturn(Optional.of(systemInfo));
        when(instanceTypeLinkRepository.findByElType("EL-A")).thenReturn(Optional.of(link));
        when(instanceTypeRepository.findByInstanceTypeId("1")).thenReturn(Optional.of(typeInfo));

        // Act
        service.changeInstanceType("test-host", InstanceType.HIGH);

        // スレッドが実行されるまで待機
        try {
            Thread.sleep(3000); // 初回の1秒 + 処理時間 + 余裕
        } catch (InterruptedException e) {
            // ignore
        }

        // Assert
        verify(systemInfoRepository).findByHostname("test-host");
        verify(instanceTypeLinkRepository).findByElType("EL-A");
        verify(instanceTypeRepository).findByInstanceTypeId("1");
        verify(instanceStatusRepository, atLeastOnce()).updateCurrentType("test-host", "HIGH");
    }

    @Test
    public void testChangeInstanceType_LOW() throws IOException {
        // Arrange
        SystemInfo systemInfo = new SystemInfo("192.168.1.1", "test-host", "EL-A", "HEL-01");
        InstanceTypeLink link = new InstanceTypeLink("EL-A", "1");
        InstanceTypeInfo typeInfo = new InstanceTypeInfo("1", "t2.xlarge", 4, "t2.medium", 2, "t2.micro", 1);

        when(systemInfoRepository.findByHostname("test-host")).thenReturn(Optional.of(systemInfo));
        when(instanceTypeLinkRepository.findByElType("EL-A")).thenReturn(Optional.of(link));
        when(instanceTypeRepository.findByInstanceTypeId("1")).thenReturn(Optional.of(typeInfo));

        // Act
        service.changeInstanceType("test-host", InstanceType.LOW);

        // スレッドが実行されるまで待機
        try {
            Thread.sleep(3000); // 初回の1秒 + 処理時間 + 余裕
        } catch (InterruptedException e) {
            // ignore
        }

        // Assert
        verify(systemInfoRepository).findByHostname("test-host");
        verify(instanceTypeLinkRepository).findByElType("EL-A");
        verify(instanceTypeRepository).findByInstanceTypeId("1");
        verify(instanceStatusRepository, atLeastOnce()).updateCurrentType("test-host", "LOW");
    }

    @Test
    public void testChangeInstanceType_SystemInfoNotFound() throws IOException {
        // Arrange
        when(systemInfoRepository.findByHostname("test-host")).thenReturn(Optional.empty());

        // Act
        service.changeInstanceType("test-host", InstanceType.HIGH);

        // Assert
        verify(systemInfoRepository).findByHostname("test-host");
        verify(instanceTypeLinkRepository, never()).findByElType(anyString());
        verify(instanceTypeRepository, never()).findByInstanceTypeId(anyString());
    }

    @Test
    public void testChangeInstanceType_InstanceTypeLinkNotFound() throws IOException {
        // Arrange
        SystemInfo systemInfo = new SystemInfo("192.168.1.1", "test-host", "EL-A", "HEL-01");
        when(systemInfoRepository.findByHostname("test-host")).thenReturn(Optional.of(systemInfo));
        when(instanceTypeLinkRepository.findByElType("EL-A")).thenReturn(Optional.empty());

        // Act
        service.changeInstanceType("test-host", InstanceType.HIGH);

        // Assert
        verify(systemInfoRepository).findByHostname("test-host");
        verify(instanceTypeLinkRepository).findByElType("EL-A");
        verify(instanceTypeRepository, never()).findByInstanceTypeId(anyString());
    }

    @Test
    public void testChangeInstanceType_InstanceTypeNotFound() throws IOException {
        // Arrange
        SystemInfo systemInfo = new SystemInfo("192.168.1.1", "test-host", "EL-A", "HEL-01");
        InstanceTypeLink link = new InstanceTypeLink("EL-A", "1");
        when(systemInfoRepository.findByHostname("test-host")).thenReturn(Optional.of(systemInfo));
        when(instanceTypeLinkRepository.findByElType("EL-A")).thenReturn(Optional.of(link));
        when(instanceTypeRepository.findByInstanceTypeId("1")).thenReturn(Optional.empty());

        // Act
        service.changeInstanceType("test-host", InstanceType.HIGH);

        // Assert
        verify(systemInfoRepository).findByHostname("test-host");
        verify(instanceTypeLinkRepository).findByElType("EL-A");
        verify(instanceTypeRepository).findByInstanceTypeId("1");
    }

    @Test
    public void testChangeInstanceType_MaxRetryReached() throws IOException {
        // Arrange
        SystemInfo systemInfo = new SystemInfo("192.168.1.1", "test-host", "EL-A", "HEL-01");
        InstanceTypeLink link = new InstanceTypeLink("EL-A", "1");
        InstanceTypeInfo typeInfo = new InstanceTypeInfo("1", "t2.xlarge", 4, "t2.medium", 2, "t2.micro", 1);

        when(systemInfoRepository.findByHostname("test-host")).thenReturn(Optional.of(systemInfo));
        when(instanceTypeLinkRepository.findByElType("EL-A")).thenReturn(Optional.of(link));
        when(instanceTypeRepository.findByInstanceTypeId("1")).thenReturn(Optional.of(typeInfo));

        // Create spy
        InstanceTypeChangeService spyService = spy(service);

        // Override config for faster test
        ReflectionTestUtils.setField(spyService, "checkIntervalSeconds", 1);
        ReflectionTestUtils.setField(spyService, "maxRetryCount", 3);

        // Mock checkInstanceTypeChangeCompletion to always return false
        doReturn(false).when(spyService).checkInstanceTypeChangeCompletion(anyString());

        // Act
        spyService.changeInstanceType("test-host", InstanceType.HIGH);

        // Wait for retries
        try {
            // 1s initial + 3 * 1s interval + buffer
            Thread.sleep(4500);
        } catch (InterruptedException e) {
            // ignore
        }

        // Assert
        // Should be called exactly maxRetryCount times (3)
        verify(spyService, times(3)).checkInstanceTypeChangeCompletion("test-host");

        // Should NOT update instance status
        verify(instanceStatusRepository, never()).updateCurrentType(anyString(), anyString());
    }

    @Test
    public void testChangeInstanceType_SuccessAfterRetries() throws IOException {
        // Arrange
        SystemInfo systemInfo = new SystemInfo("192.168.1.1", "test-host", "EL-A", "HEL-01");
        InstanceTypeLink link = new InstanceTypeLink("EL-A", "1");
        InstanceTypeInfo typeInfo = new InstanceTypeInfo("1", "t2.xlarge", 4, "t2.medium", 2, "t2.micro", 1);

        when(systemInfoRepository.findByHostname("test-host")).thenReturn(Optional.of(systemInfo));
        when(instanceTypeLinkRepository.findByElType("EL-A")).thenReturn(Optional.of(link));
        when(instanceTypeRepository.findByInstanceTypeId("1")).thenReturn(Optional.of(typeInfo));

        // Create spy
        InstanceTypeChangeService spyService = spy(service);

        // Override config for faster test
        ReflectionTestUtils.setField(spyService, "checkIntervalSeconds", 1);
        ReflectionTestUtils.setField(spyService, "maxRetryCount", 3);

        // Mock checkInstanceTypeChangeCompletion to return false twice, then true
        doReturn(false).doReturn(false).doReturn(true).when(spyService).checkInstanceTypeChangeCompletion(anyString());

        // Act
        spyService.changeInstanceType("test-host", InstanceType.HIGH);

        // Wait for retries
        try {
            // 1s initial + 2 * 1s interval + buffer
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            // ignore
        }

        // Assert
        // Should be called 3 times (2 failures + 1 success)
        verify(spyService, times(3)).checkInstanceTypeChangeCompletion("test-host");

        // Should update instance status
        verify(instanceStatusRepository, times(1)).updateCurrentType("test-host", "HIGH");
    }
}
