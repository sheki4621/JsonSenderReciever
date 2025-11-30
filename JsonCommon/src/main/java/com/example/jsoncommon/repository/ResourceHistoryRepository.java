package com.example.jsoncommon.repository;

import com.example.jsoncommon.dto.InstanceTypeChangeRequest;
import com.example.jsoncommon.dto.MetricsJson;
import com.example.jsoncommon.dto.ResourceHistoryCsv;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ResourceHistoryRepository extends CsvRepositoryBase {

    private static final String[] HEADERS = { "Hostname", "Timestamp", "CpuUsage", "MemoryUsage",
            "InstanceTypeChangeRequest" };

    @Value("${resource.history.retention-days:30}")
    private int retentionDays;

    /**
     * 保持期間を設定する（テスト用）
     * 
     * @param retentionDays 保持期間（日数）
     */
    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    /**
     * 保持期間を取得する
     * 
     * @return 保持期間（日数）
     */
    public int getRetentionDays() {
        return retentionDays;
    }

    public void save(MetricsJson metricsJson) throws IOException {
        String filename = String.format("resource_history_%s.csv", metricsJson.getInstanceName());
        writeToCsv(filename, HEADERS,
                metricsJson.getInstanceName(),
                metricsJson.getTimestamp(),
                metricsJson.getMetrics().getCpuUsage(),
                metricsJson.getMetrics().getMemoryUsage(),
                metricsJson.getMetrics().getInstanceTypeChangeRequest());

        // 保存後に古いデータを自動削除
        deleteOldRecords(metricsJson.getInstanceName());
    }

    /**
     * 指定ホスト名の直近N分間のリソース情報を取得する
     * 
     * @param hostname ホスト名
     * @param minutes  取得分
     * @return リソース情報のリスト（最新のものから順）
     * @throws IOException IO例外
     */
    public List<ResourceHistoryCsv> findRecentByHostname(String hostname, int minutes) throws IOException {
        String filename = String.format("resource_history_%s.csv", hostname);

        List<String> lines = readFromCsv(filename);
        List<ResourceHistoryCsv> allMatching = new ArrayList<>();

        if (lines.isEmpty()) {
            return allMatching;
        }

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime thresholdTime = now.minusMinutes(minutes);

        // 逆順から取得
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            String[] parts = line.split(",", -1);

            // ヘッダー行はスキップ
            if (parts[0].equals("Hostname")) {
                continue;
            }

            if (parts.length >= 5 && parts[0].equals(hostname)) {
                try {
                    ZonedDateTime timestamp = ZonedDateTime.parse(parts[1]);

                    // 指定期間より古いデータになったら終了（CSVが時系列順であることを前提）
                    if (timestamp.isBefore(thresholdTime)) {
                        break;
                    }

                    ResourceHistoryCsv info = new ResourceHistoryCsv(
                            parts[0], // hostname
                            parts[1], // timestamp
                            Double.parseDouble(parts[2]), // cpuUsage
                            Double.parseDouble(parts[3]), // memoryUsage
                            InstanceTypeChangeRequest.valueOf(parts[4]) // instanceTypeChangeRequest
                    );
                    allMatching.add(info);
                } catch (Exception e) {
                    // パースエラー等は無視して次へ
                    continue;
                }
            }
        }

        // タイムスタンプでソート（降順:最新が先頭）
        allMatching.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        return allMatching;
    }

    /**
     * 保持期間を過ぎた古いレコードを削除する
     * 
     * @param hostname ホスト名
     * @throws IOException IO例外
     */
    public void deleteOldRecords(String hostname) throws IOException {
        String filename = String.format("resource_history_%s.csv", hostname);

        List<String> lines = readFromCsv(filename);

        // ファイルが存在しない、または空の場合は何もしない
        if (lines.isEmpty()) {
            return;
        }

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime thresholdTime = now.minusDays(retentionDays);

        List<Object[]> recordsToKeep = new ArrayList<>();

        for (String line : lines) {
            String[] parts = line.split(",", -1);

            // ヘッダー行はスキップ（上書き時に自動追加される）
            if (parts[0].equals("Hostname")) {
                continue;
            }

            if (parts.length >= 5) {
                try {
                    ZonedDateTime timestamp = ZonedDateTime.parse(parts[1]);

                    // 保持期間内のデータのみを保持
                    if (!timestamp.isBefore(thresholdTime)) {
                        recordsToKeep.add(new Object[] {
                                parts[0], // hostname
                                parts[1], // timestamp
                                parts[2], // cpuUsage
                                parts[3], // memoryUsage
                                parts[4] // instanceTypeChangeRequest
                        });
                    }
                } catch (Exception e) {
                    // パースエラー等は無視して次へ
                    continue;
                }
            }
        }

        // 保持するレコードでCSVを上書き
        overwriteToCsv(filename, HEADERS, recordsToKeep);
    }

}
