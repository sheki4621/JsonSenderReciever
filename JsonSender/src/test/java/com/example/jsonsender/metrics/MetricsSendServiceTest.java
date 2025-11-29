package com.example.jsonsender.metrics;

import com.example.jsoncommon.dto.Metrics;
import com.example.jsoncommon.util.ShellExecutor;
import com.example.jsonsender.repository.ThresholdRepository;
import com.example.jsoncommon.repository.ResourceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MetricsSendServiceのテストクラス
 */
class MetricsSendServiceTest {

    @Mock
    private ThresholdRepository thresholdRepository;

    @Mock
    private ResourceHistoryRepository resourceHistoryRepository;

    @Mock
    private ShellExecutor shellExecutor;

    private MetricsSendService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new MetricsSendService(thresholdRepository, resourceHistoryRepository, shellExecutor);

        // シェルパスとタイムアウトを設定
        ReflectionTestUtils.setField(service, "metricsShellPath", "/path/to/metrics.sh");
        ReflectionTestUtils.setField(service, "shellTimeoutSeconds", 30);
    }

    @Test
    void testGetCpuMemoryUsage_正常系_シェル実行成功() throws Exception {
        // Arrange
        String shellOutput = "{\"CpuUsage\": 23.4, \"MemoryUsage\": 34.5}\n";
        when(shellExecutor.executeShell(anyString(), anyList(), anyInt())).thenReturn(shellOutput);

        // Act
        Metrics result = service.getCpuMemoryUsage();

        // Assert
        assertNotNull(result);
        assertEquals(23.4, result.getCpuUsage());
        assertEquals(34.5, result.getMemoryUsage());
        assertNull(result.getInstanceTypeChangeRequest());
    }

    @Test
    void testGetCpuMemoryUsage_異常系_シェル実行失敗() throws Exception {
        // Arrange
        when(shellExecutor.executeShell(anyString(), anyList(), anyInt()))
                .thenThrow(new IOException("シェルが非ゼロで終了しました"));

        // Act
        Metrics result = service.getCpuMemoryUsage();

        // Assert
        assertNotNull(result);
        assertNull(result.getCpuUsage());
        assertNull(result.getMemoryUsage());
        assertNull(result.getInstanceTypeChangeRequest());
    }

    @Test
    void testGetCpuMemoryUsage_異常系_タイムアウト() throws Exception {
        // Arrange
        when(shellExecutor.executeShell(anyString(), anyList(), anyInt()))
                .thenThrow(new TimeoutException("シェル実行がタイムアウトしました"));

        // Act
        Metrics result = service.getCpuMemoryUsage();

        // Assert
        assertNotNull(result);
        assertNull(result.getCpuUsage());
        assertNull(result.getMemoryUsage());
        assertNull(result.getInstanceTypeChangeRequest());
    }

    @Test
    void testGetCpuMemoryUsage_異常系_JSONパース失敗() throws Exception {
        // Arrange
        String invalidJson = "invalid json format";
        when(shellExecutor.executeShell(anyString(), anyList(), anyInt())).thenReturn(invalidJson);

        // Act
        Metrics result = service.getCpuMemoryUsage();

        // Assert
        assertNotNull(result);
        assertNull(result.getCpuUsage());
        assertNull(result.getMemoryUsage());
        assertNull(result.getInstanceTypeChangeRequest());
    }

    @Test
    void testGetCpuMemoryUsage_異常系_JSONフィールド不足() throws Exception {
        // Arrange
        String incompleteJson = "{\"CpuUsage\": 23.4}\n";
        when(shellExecutor.executeShell(anyString(), anyList(), anyInt())).thenReturn(incompleteJson);

        // Act
        Metrics result = service.getCpuMemoryUsage();

        // Assert
        assertNotNull(result);
        assertEquals(23.4, result.getCpuUsage());
        assertNull(result.getMemoryUsage());
        assertNull(result.getInstanceTypeChangeRequest());
    }

    @Test
    void testGetCpuMemoryUsage_異常系_空の出力() throws Exception {
        // Arrange
        when(shellExecutor.executeShell(anyString(), anyList(), anyInt())).thenReturn("");

        // Act
        Metrics result = service.getCpuMemoryUsage();

        // Assert
        assertNotNull(result);
        assertNull(result.getCpuUsage());
        assertNull(result.getMemoryUsage());
        assertNull(result.getInstanceTypeChangeRequest());
    }

    @Test
    void testGetCpuMemoryUsage_正常系_小数値() throws Exception {
        // Arrange
        String shellOutput = "{\"CpuUsage\": 99.9, \"MemoryUsage\": 0.1}\n";
        when(shellExecutor.executeShell(anyString(), anyList(), anyInt())).thenReturn(shellOutput);

        // Act
        Metrics result = service.getCpuMemoryUsage();

        // Assert
        assertNotNull(result);
        assertEquals(99.9, result.getCpuUsage());
        assertEquals(0.1, result.getMemoryUsage());
        assertNull(result.getInstanceTypeChangeRequest());
    }
}
