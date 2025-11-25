package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.InstanceTypeInfo;
import com.example.jsonreceiver.dto.SystemInfo;
import com.example.jsonreceiver.repository.InstanceTypeRepository;
import com.example.jsonreceiver.repository.SystemInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new InformationCollectionService(instanceTypeRepository, systemInfoRepository);
        // リトライ設定を注入
        ReflectionTestUtils.setField(service, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(service, "retryIntervalSeconds", 1);
    }

    @Test
    void testCollectInstanceTypes_returnsNonEmptyList() throws Exception {
        // インスタンスタイプ一覧の取得
        List<InstanceTypeInfo> result = service.collectInstanceTypes();

        // 結果が空でないことを確認
        assertNotNull(result);
        assertFalse(result.isEmpty());

        // サンプルデータの構造を確認
        InstanceTypeInfo first = result.get(0);
        assertNotNull(first);
        assertNotNull(first.getInstanceType());
        assertFalse(first.getInstanceType().isEmpty());

        // CSV出力が呼ばれたことを確認
        verify(instanceTypeRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testCollectSystemInfo_returnsNonEmptyList() throws Exception {
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
    }

    @Test
    void testCollectInstanceTypes_containsExpectedTypes() throws Exception {
        // インスタンスタイプ一覧の取得
        List<InstanceTypeInfo> result = service.collectInstanceTypes();

        // 期待されるサンプルデータが含まれることを確認
        assertTrue(result.stream()
                .anyMatch(info -> "t2.micro".equals(info.getInstanceType())));

        // CSV出力が呼ばれたことを確認
        verify(instanceTypeRepository, times(1)).saveAll(result);
    }

    @Test
    void testCollectSystemInfo_containsValidData() throws Exception {
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
}
