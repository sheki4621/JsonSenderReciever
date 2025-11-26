package com.example.jsonreceiver.repository;

import com.example.jsonreceiver.dto.ResourceInfo;
import org.springframework.stereotype.Repository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ResourceInfoRepository extends CsvRepositoryBase {

    private static final String FILE_NAME = "ResourceInfo.csv";
    private static final String[] HEADERS = { "Hostname", "Timestamp", "CpuUsage", "MemoryUsage" };

    public void save(String hostname, String timestamp, String cpuUsage, String memoryUsage) throws IOException {
        writeToCsv(FILE_NAME, HEADERS, hostname, timestamp, cpuUsage, memoryUsage);
    }

    /**
     * 指定ホスト名の最新N件のリソース情報を取得する
     * 受信情報がソートされている保証がないため、念の為n*5件取得してソートし、最新n件を返す
     * 
     * @param hostname ホスト名
     * @param n        取得件数
     * @return リソース情報のリスト（最新のものから順）
     * @throws IOException IO例外
     */
    public List<ResourceInfo> findLastNByHostname(String hostname, int n) throws IOException {
        List<String> lines = readFromCsv(FILE_NAME);
        List<ResourceInfo> allMatching = new ArrayList<>();

        if (lines.isEmpty()) {
            return allMatching;
        }

        // ヘッダーをスキップして、該当ホスト名のレコードを最大 n*5 件取得
        // n*5件を取得してソートすることで、最新n件を取得する確率を高める(本番はSQLでソートするので問題ない想定)
        int maxRecords = n * 5;
        for (int i = 1; i < lines.size() && allMatching.size() < maxRecords; i++) {
            String line = lines.get(i);
            String[] parts = line.split(",", -1);
            if (parts.length >= 4 && parts[0].equals(hostname)) {
                ResourceInfo info = new ResourceInfo(
                        parts[0], // hostname
                        parts[1], // timestamp
                        Double.parseDouble(parts[2]), // cpuUsage
                        Double.parseDouble(parts[3]) // memoryUsage
                );
                allMatching.add(info);
            }
        }

        // タイムスタンプでソート（降順：最新が先頭）
        allMatching.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        // 最新n件を返す
        return allMatching.size() <= n ? allMatching : allMatching.subList(0, n);
    }
}
