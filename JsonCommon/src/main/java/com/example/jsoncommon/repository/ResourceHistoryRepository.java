package com.example.jsoncommon.repository;

import com.example.jsoncommon.dto.InstanceTypeChangeRequest;
import com.example.jsoncommon.dto.MetricsJson;
import com.example.jsoncommon.dto.ResourceHistoryCsv;

import org.springframework.stereotype.Repository;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ResourceHistoryRepository extends CsvRepositoryBase {

    private static final String[] HEADERS = { "Hostname", "Timestamp", "CpuUsage", "MemoryUsage",
            "InstanceTypeChangeRequest" };

    public void save(MetricsJson metricsJson) throws IOException {
        String filename = String.format("resource_history_%s.csv", metricsJson.getInstanceName());
        writeToCsv(filename, HEADERS,
                metricsJson.getInstanceName(),
                metricsJson.getTimestamp(),
                metricsJson.getMetrics().getCpuUsage(),
                metricsJson.getMetrics().getMemoryUsage(),
                metricsJson.getMetrics().getInstanceTypeChangeRequest());
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

        // タイムスタンプでソート（降順：最新が先頭）
        allMatching.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        return allMatching;
    }

    /**
     * 指定ホスト名の直近N件のリソース情報を取得する
     * TODO: いらないかも
     * 
     * @param hostname ホスト名
     * @param n        取得件数
     * @return リソース情報のリスト（最新のものから順）
     * @throws IOException IO例外
     */
    public List<ResourceHistoryCsv> findLastNByHostname(String hostname, int n) throws IOException {
        String filename = String.format("resource_history_%s.csv", hostname);

        List<String> lines = readFromCsv(filename);
        List<ResourceHistoryCsv> allMatching = new ArrayList<>();

        if (lines.isEmpty()) {
            return allMatching;
        }

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
                    ResourceHistoryCsv info = new ResourceHistoryCsv(
                            parts[0], // hostname
                            parts[1], // timestamp
                            Double.parseDouble(parts[2]), // cpuUsage
                            Double.parseDouble(parts[3]), // memoryUsage
                            InstanceTypeChangeRequest.valueOf(parts[4]) // instanceTypeChangeRequest
                    );
                    allMatching.add(info);

                    if (allMatching.size() >= n) {
                        break;
                    }
                } catch (Exception e) {
                    // パースエラー等は無視して次へ
                    continue;
                }
            }
        }

        // タイムスタンプでソート（降順：最新が先頭）
        allMatching.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        return allMatching;
    }
}
