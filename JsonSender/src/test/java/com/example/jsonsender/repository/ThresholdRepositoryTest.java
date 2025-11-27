package com.example.jsonsender.repository;

import com.example.jsoncommon.dto.ConditionLogic;
import com.example.jsoncommon.dto.ThresholdInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ThresholdRepositoryTest {

    private ThresholdRepository repository;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        repository = new ThresholdRepository();
        repository.setOutputDir(tempDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        // テスト後のクリーンアップ
        Path thresholdFile = tempDir.resolve(repository.getFilePath());
        if (Files.exists(thresholdFile)) {
            Files.delete(thresholdFile);
        }
    }

    @Test
    void testSaveThresholdInfo() throws IOException {
        // Given: しきい値情報を作成
        ThresholdInfo thresholdInfo = new ThresholdInfo(
                "test-host",
                null, // scalingMode
                true, // upperChangeableEnable
                80.0, // upperCpuThreshold
                5, // upperCpuDurationMin
                85.0, // upperMemThreshold
                5, // upperMemDurationMin
                ConditionLogic.OR, // upperConditionLogic
                true, // lowerChangeableEnable
                20.0, // lowerCpuThreshold
                10, // lowerCpuDurationMin
                25.0, // lowerMemThreshold
                10, // lowerMemDurationMin
                ConditionLogic.OR, // lowerConditionLogic
                false, // microChangeableEnable
                false // microForceOnStandby
        );

        // When: ファイルに保存
        repository.save(thresholdInfo);

        // Then: ファイルが作成され、内容が正しいことを確認
        // Then: ファイルが作成され、内容が正しいことを確認
        Path thresholdFile = tempDir.resolve(repository.getFilePath());
        assertTrue(Files.exists(thresholdFile), "しきい値ファイルが作成されていません");

        String content = Files.readString(thresholdFile);
        assertTrue(content.contains("test-host"), "ホスト名が含まれていません");
        assertTrue(content.contains("80.0"), "CPU上限しきい値が含まれていません");
        assertTrue(content.contains("85.0"), "メモリ上限しきい値が含まれていません");
    }

    @Test
    void testFindByHostname_Success() throws IOException {
        // Given: しきい値情報を保存
        ThresholdInfo savedInfo = new ThresholdInfo(
                "test-host",
                null, // scalingMode
                true, // upperChangeableEnable
                80.0, // upperCpuThreshold
                5, // upperCpuDurationMin
                85.0, // upperMemThreshold
                5, // upperMemDurationMin
                ConditionLogic.OR, // upperConditionLogic
                true, // lowerChangeableEnable
                20.0, // lowerCpuThreshold
                10, // lowerCpuDurationMin
                25.0, // lowerMemThreshold
                10, // lowerMemDurationMin
                ConditionLogic.OR, // lowerConditionLogic
                false, // microChangeableEnable
                false // microForceOnStandby
        );
        repository.save(savedInfo);

        // When: ホスト名で検索
        Optional<ThresholdInfo> result = repository.findByHostname("test-host");

        // Then: しきい値情報が取得できることを確認
        assertTrue(result.isPresent(), "しきい値情報が見つかりませんでした");
        ThresholdInfo info = result.get();
        assertEquals("test-host", info.getHostname());
        assertEquals(80.0, info.getUpperCpuThreshold());
        assertEquals(85.0, info.getUpperMemThreshold());
        assertEquals(ConditionLogic.OR, info.getUpperConditionLogic());
        assertEquals(20.0, info.getLowerCpuThreshold());
        assertEquals(25.0, info.getLowerMemThreshold());
        assertEquals(ConditionLogic.OR, info.getLowerConditionLogic());
    }

    @Test
    void testFindByHostname_NotFound() throws IOException {
        // Given: しきい値情報を保存
        ThresholdInfo savedInfo = new ThresholdInfo(
                "test-host",
                null, // scalingMode
                true, // upperChangeableEnable
                80.0, // upperCpuThreshold
                5, // upperCpuDurationMin
                85.0, // upperMemThreshold
                5, // upperMemDurationMin
                ConditionLogic.OR, // upperConditionLogic
                true, // lowerChangeableEnable
                20.0, // lowerCpuThreshold
                10, // lowerCpuDurationMin
                25.0, // lowerMemThreshold
                10, // lowerMemDurationMin
                ConditionLogic.OR, // lowerConditionLogic
                false, // microChangeableEnable
                false // microForceOnStandby
        );
        repository.save(savedInfo);

        // When: 存在しないホスト名で検索
        Optional<ThresholdInfo> result = repository.findByHostname("non-existent-host");

        // Then: Optional.empty()が返されることを確認
        assertFalse(result.isPresent(), "存在しないホスト名でしきい値情報が見つかりました");
    }

    @Test
    void testFindByHostname_FileNotExists() throws IOException {
        // When: ファイルが存在しない状態で検索
        Optional<ThresholdInfo> result = repository.findByHostname("test-host");

        // Then: Optional.empty()が返されることを確認
        assertFalse(result.isPresent(), "ファイルが存在しない場合、しきい値情報が見つからないべきです");
    }
}
