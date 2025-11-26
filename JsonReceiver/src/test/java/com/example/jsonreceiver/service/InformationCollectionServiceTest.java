package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.InstanceTypeInfo;
import com.example.jsonreceiver.dto.SystemInfo;
import com.example.jsonreceiver.repository.InstanceTypeRepository;
import com.example.jsonreceiver.repository.SystemInfoRepository;
import com.example.jsonreceiver.util.ShellExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InformationCollectionServiceのテストクラス
 */
class InformationCollectionServiceTest {

    private InformationCollectionService service;

    @Mock
    private InstanceTypeRepository instanceTypeRepository;

    @Mock
    private SystemInfoRepository systemInfoRepository;

    @Mock
    private ShellExecutor shellExecutor;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper(); // 実際のObjectMapperを使用
        service = new InformationCollectionService(
                instanceTypeRepository,
                systemInfoRepository,
                shellExecutor,
                objectMapper);
        // リトライ設定を注入
        ReflectionTestUtils.setField(service, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(service, "retryIntervalSeconds", 1);
        // シェルパスとタイムアウトを注入
        ReflectionTestUtils.setField(service, "systemInfoShellPath", "/path/to/get_system_info.sh");
        ReflectionTestUtils.setField(service, "instanceTypeShellPath", "/path/to/get_instance_type.sh");
        ReflectionTestUtils.setField(service, "shellTimeoutSeconds", 30);
    }

    @Test
    void testCollectInstanceTypes_returnsNonEmptyList() throws Exception {
        // シェル実行時のJSONレスポンスをモック
        String jsonResponse = "[{\"instanceTypeId\":\"1\",\"highInstanceType\":\"t2.xlarge\",\"highCpuCore\":4,\"lowInstanceType\":\"t2.medium\",\"lowCpuCore\":2,\"veryLowInstanceType\":\"t2.micro\",\"veryLowCpuCore\":1}]";
        when(shellExecutor.executeShell(anyString(), anyList(), anyInt())).thenReturn(jsonResponse);

        // インスタンスタイプ一覧の取得
        List<InstanceTypeInfo> result = service.collectInstanceTypes();

        // 結果が空でないことを確認
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // サンプルデータの構造を確認
        InstanceTypeInfo first = result.get(0);
        assertNotNull(first);
        assertNotNull(first.getInstanceTypeId());
        assertFalse(first.getInstanceTypeId().isEmpty());

        // CSV出力が呼ばれたことを確認
        verify(instanceTypeRepository, times(1)).saveAll(anyList());
        // シェルが実行されたことを確認
        verify(shellExecutor, times(1)).executeShell(anyString(), anyList(), anyInt());
    }

    @Test
    void testCollectSystemInfo_returnsNonEmptyList() throws Exception {
        // シェル実行時のJSONレスポンスをモック
        String jsonResponse = "[{\"ipAddress\":\"192.168.1.10\",\"hostname\":\"server01.example.com\",\"elType\":\"EL-A\",\"helName\":\"HEL-XXX-01\"}]";
        when(shellExecutor.executeShell(anyString(), anyList(), anyInt())).thenReturn(jsonResponse);

        // システム情報の取得
        List<SystemInfo> result = service.collectSystemInfo();

        // 結果が空でないことを確認
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // サンプルデータの構造を確認
        SystemInfo first = result.get(0);
        assertNotNull(first);
        assertNotNull(first.getIpAddress());
        assertNotNull(first.getHostname());
        assertNotNull(first.getElType());
        assertNotNull(first.getHelName());
        assertFalse(first.getIpAddress().isEmpty());

        // CSV出力が呼ばれたことを確認
        verify(systemInfoRepository, times(1)).saveAll(anyList());
        // シェルが実行されたことを確認
        verify(shellExecutor, times(1)).executeShell(anyString(), anyList(), anyInt());
    }

    @Test
    void testCollectInstanceTypes_containsExpectedTypes() throws Exception {
        // シェル実行時のJSONレスポンスをモック
        String jsonResponse = "[{\"instanceTypeId\":\"1\",\"highInstanceType\":\"t2.xlarge\",\"highCpuCore\":4,\"lowInstanceType\":\"t2.medium\",\"lowCpuCore\":2,\"veryLowInstanceType\":\"t2.micro\",\"veryLowCpuCore\":1}]";
        when(shellExecutor.executeShell(anyString(), anyList(), anyInt())).thenReturn(jsonResponse);

        // インスタンスタイプ一覧の取得
        List<InstanceTypeInfo> result = service.collectInstanceTypes();

        // 期待されるサンプルデータが含まれることを確認
        assertTrue(result.stream()
                .anyMatch(info -> "t2.xlarge".equals(info.getHighInstanceType())));

        // CSV出力が呼ばれたことを確認
        verify(instanceTypeRepository, times(1)).saveAll(result);
    }

    @Test
    void testCollectSystemInfo_containsValidData() throws Exception {
        // シェル実行時のJSONレスポンスをモック
        String jsonResponse = "[{\"ipAddress\":\"192.168.1.10\",\"hostname\":\"server01.example.com\",\"elType\":\"EL-A\",\"helName\":\"HEL-XXX-01\"}]";
        when(shellExecutor.executeShell(anyString(), anyList(), anyInt())).thenReturn(jsonResponse);

        // システム情報の取得
        List<SystemInfo> result = service.collectSystemInfo();

        // 期待されるサンプルデータが含まれることを確認
        assertTrue(result.stream()
                .anyMatch(info -> info.getIpAddress() != null &&
                        info.getIpAddress().matches("\\d+\\.\\d+\\.\\d+\\.\\d+")));

        // CSV出力が呼ばれたことを確認
        verify(systemInfoRepository, times(1)).saveAll(result);
    }

    @Test
    void testCollectInstanceTypes_continuesOnCsvError() throws Exception {
        // シェル実行時のJSONレスポンスをモック
        String jsonResponse = "[{\"instanceTypeId\":\"1\",\"highInstanceType\":\"t2.xlarge\",\"highCpuCore\":4,\"lowInstanceType\":\"t2.medium\",\"lowCpuCore\":2,\"veryLowInstanceType\":\"t2.micro\",\"veryLowCpuCore\":1}]";
        when(shellExecutor.executeShell(anyString(), anyList(), anyInt())).thenReturn(jsonResponse);

        // CSV出力時にエラーが発生するようモックを設定
        doThrow(new RuntimeException("CSV書き込みエラー"))
                .when(instanceTypeRepository).saveAll(anyList());

        // データは正常に取得できることを確認（CSV出力のエラーは無視される）
        List<InstanceTypeInfo> result = service.collectInstanceTypes();
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // CSV出力が試行されたことを確認
        verify(instanceTypeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testCollectSystemInfo_continuesOnCsvError() throws Exception {
        // シェル実行時のJSONレスポンスをモック
        String jsonResponse = "[{\"ipAddress\":\"192.168.1.10\",\"hostname\":\"server01.example.com\",\"elType\":\"EL-A\",\"helName\":\"HEL-XXX-01\"}]";
        when(shellExecutor.executeShell(anyString(), anyList(), anyInt())).thenReturn(jsonResponse);

        // CSV出力時にエラーが発生するようモックを設定
        doThrow(new RuntimeException("CSV書き込みエラー"))
                .when(systemInfoRepository).saveAll(anyList());

        // データは正常に取得できることを確認（CSV出力のエラーは無視される）
        List<SystemInfo> result = service.collectSystemInfo();
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // CSV出力が試行されたことを確認
        verify(systemInfoRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testCollectInstanceTypes_fallbackOnShellError() throws Exception {
        // シェル実行失敗時のモック
        when(shellExecutor.executeShell(anyString(), anyList(), anyInt()))
                .thenThrow(new RuntimeException("シェル実行エラー"));

        // フォールバックでサンプルデータが返されることを確認
        List<InstanceTypeInfo> result = service.collectInstanceTypes();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.stream()
                .anyMatch(info -> "t2.xlarge".equals(info.getHighInstanceType())));

        // CSV出力が試行されたことを確認
        verify(instanceTypeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testCollectSystemInfo_fallbackOnShellError() throws Exception {
        // シェル実行失敗時のモック
        when(shellExecutor.executeShell(anyString(), anyList(), anyInt()))
                .thenThrow(new RuntimeException("シェル実行エラー"));

        // フォールバックでサンプルデータが返されることを確認
        List<SystemInfo> result = service.collectSystemInfo();
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.stream()
                .anyMatch(info -> "192.168.1.10".equals(info.getIpAddress())));

        // CSV出力が試行されたことを確認
        verify(systemInfoRepository, times(1)).saveAll(anyList());
    }
}
