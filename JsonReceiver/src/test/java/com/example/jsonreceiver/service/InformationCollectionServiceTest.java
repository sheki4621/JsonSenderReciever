package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.InstanceTypeInfo;
import com.example.jsonreceiver.dto.SystemInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InformationCollectionServiceのテストクラス
 */
class InformationCollectionServiceTest {

    private InformationCollectionService service;

    @BeforeEach
    void setUp() {
        service = new InformationCollectionService();
        // リトライ設定を注入
        ReflectionTestUtils.setField(service, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(service, "retryIntervalSeconds", 1);
    }

    @Test
    void testCollectInstanceTypes_returnsNonEmptyList() {
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
    }

    @Test
    void testCollectSystemInfo_returnsNonEmptyList() {
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
    }

    @Test
    void testCollectInstanceTypes_containsExpectedTypes() {
        // インスタンスタイプ一覧の取得
        List<InstanceTypeInfo> result = service.collectInstanceTypes();

        // 期待されるサンプルデータが含まれることを確認
        assertTrue(result.stream()
                .anyMatch(info -> "t2.micro".equals(info.getInstanceType())));
    }

    @Test
    void testCollectSystemInfo_containsValidData() {
        // システム情報の取得
        List<SystemInfo> result = service.collectSystemInfo();

        // 期待されるサンプルデータが含まれることを確認
        assertTrue(result.stream()
                .anyMatch(info -> info.getIpAddress() != null &&
                        info.getIpAddress().matches("\\d+\\.\\d+\\.\\d+\\.\\d+")));
    }
}
