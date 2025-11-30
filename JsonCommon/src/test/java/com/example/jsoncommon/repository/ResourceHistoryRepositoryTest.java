package com.example.jsoncommon.repository;

import com.example.jsoncommon.dto.InstanceTypeChangeRequest;
import com.example.jsoncommon.dto.Metrics;
import com.example.jsoncommon.dto.MetricsJson;
import com.example.jsoncommon.dto.NoticeType;
import com.example.jsoncommon.dto.ResourceHistoryCsv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceHistoryRepositoryTest {

    @TempDir
    Path tempDir;

    private ResourceHistoryRepository repository;
    private Path csvDir;

    @BeforeEach
    public void setUp() throws IOException {
        csvDir = tempDir.resolve("csv");
        Files.createDirectories(csvDir);

        repository = new ResourceHistoryRepository();
        repository.setOutputDir(csvDir.toString());
        repository.setRetentionDays(30); // デフォルト値を設定
    }

    @Test
    public void testSave() throws IOException {
        // Arrange
        MetricsJson metrics = createMetricsJson("test-host", ZonedDateTime.now(), 75.0, 60.0);

        // Act
        repository.save(metrics);

        // Assert
        Path csvFilePath = csvDir.resolve("resource_history_test-host.csv");
        assertTrue(Files.exists(csvFilePath));
        List<String> lines = Files.readAllLines(csvFilePath);
        assertTrue(lines.size() >= 2);
        assertEquals("Hostname,Timestamp,CpuUsage,MemoryUsage,InstanceTypeChangeRequest", lines.get(0));
        assertTrue(lines.get(1).startsWith("test-host,"));
    }

    @Test
    public void testFindRecentByHostname() throws IOException {
        // Arrange
        String hostname = "test-host";
        ZonedDateTime now = ZonedDateTime.now();

        // 5分前、3分前、1分前のデータを保存
        repository.save(createMetricsJson(hostname, now.minusMinutes(5), 70.0, 50.0));
        repository.save(createMetricsJson(hostname, now.minusMinutes(3), 75.0, 55.0));
        repository.save(createMetricsJson(hostname, now.minusMinutes(1), 80.0, 60.0));

        // Act: 直近4分間のデータを取得
        List<ResourceHistoryCsv> result = repository.findRecentByHostname(hostname, 4);

        // Assert: 3分前と1分前の2件が取得される
        assertEquals(2, result.size());
        assertEquals(80.0, result.get(0).getCpuUsage()); // 最新が先頭
        assertEquals(75.0, result.get(1).getCpuUsage());
    }

    @Test
    public void testDeleteOldRecords_保持期間を過ぎたデータが削除される() throws IOException {
        // Arrange
        String hostname = "test-host";
        ZonedDateTime now = ZonedDateTime.now();

        // 保持期間を1日に設定
        repository.setRetentionDays(1);

        // 3日前、2日前、12時間前のデータを保存
        repository.save(createMetricsJson(hostname, now.minusDays(3), 60.0, 40.0));
        repository.save(createMetricsJson(hostname, now.minusDays(2), 70.0, 50.0));
        repository.save(createMetricsJson(hostname, now.minusHours(12), 80.0, 60.0));

        // Act: 古いデータを削除
        repository.deleteOldRecords(hostname);

        // Assert: 12時間前のデータのみが残る
        Path csvFilePath = csvDir.resolve("resource_history_test-host.csv");
        List<String> lines = Files.readAllLines(csvFilePath);

        // ヘッダー + 1行のデータ
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains("80.0")); // 12時間前のCPU使用率
    }

    @Test
    public void testDeleteOldRecords_保持期間内のデータは削除されない() throws IOException {
        // Arrange
        String hostname = "test-host";
        ZonedDateTime now = ZonedDateTime.now();

        // 保持期間を30日に設定
        repository.setRetentionDays(30);

        // 10日前、5日前、1日前のデータを保存（全て保持期間内）
        repository.save(createMetricsJson(hostname, now.minusDays(10), 60.0, 40.0));
        repository.save(createMetricsJson(hostname, now.minusDays(5), 70.0, 50.0));
        repository.save(createMetricsJson(hostname, now.minusDays(1), 80.0, 60.0));

        // Act: 古いデータを削除
        repository.deleteOldRecords(hostname);

        // Assert: 全てのデータが残る
        Path csvFilePath = csvDir.resolve("resource_history_test-host.csv");
        List<String> lines = Files.readAllLines(csvFilePath);

        // ヘッダー + 3行のデータ
        assertEquals(4, lines.size());
    }

    @Test
    public void testSaveWithAutoDeletion_保存時に古いデータが自動削除される() throws IOException {
        // Arrange
        String hostname = "test-host";
        ZonedDateTime now = ZonedDateTime.now();

        // 保持期間を1日に設定
        repository.setRetentionDays(1);

        // 5日前のデータを保存
        repository.save(createMetricsJson(hostname, now.minusDays(5), 60.0, 40.0));

        // Act: 新しいデータを保存（自動削除が実行される）
        repository.save(createMetricsJson(hostname, now, 80.0, 60.0));

        // Assert: 古いデータは削除され、新しいデータのみが残る
        Path csvFilePath = csvDir.resolve("resource_history_test-host.csv");
        List<String> lines = Files.readAllLines(csvFilePath);

        // ヘッダー + 1行のデータ
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains("80.0")); // 新しいデータのCPU使用率
    }

    @Test
    public void testDeleteOldRecords_CSVファイルが存在しない場合はエラーにならない() throws IOException {
        // Arrange
        String hostname = "non-existent-host";
        repository.setRetentionDays(30);

        // Act & Assert: エラーが発生しないことを確認
        assertDoesNotThrow(() -> repository.deleteOldRecords(hostname));
    }

    @Test
    public void testSetRetentionDays() {
        // Act
        repository.setRetentionDays(10);

        // Assert
        assertEquals(10, repository.getRetentionDays());
    }

    // ヘルパーメソッド
    private MetricsJson createMetricsJson(String hostname, ZonedDateTime timestamp,
            double cpuUsage, double memoryUsage) {
        return new MetricsJson(
                UUID.randomUUID(),
                NoticeType.METRICS,
                timestamp,
                "1.0.0",
                hostname,
                new Metrics(cpuUsage, memoryUsage, InstanceTypeChangeRequest.WITHIN));
    }
}
