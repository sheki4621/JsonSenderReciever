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
     * 
     * @param hostname ホスト名
     * @param n        取得件数
     * @return リソース情報のリスト（最新のものから順）
     * @throws IOException IO例外
     */
    public List<ResourceInfo> findLastNByHostname(String hostname, int n) throws IOException {
        List<String> lines = readFromCsv(FILE_NAME);
        List<ResourceInfo> result = new ArrayList<>();

        if (lines.isEmpty()) {
            return result;
        }

        // ヘッダーをスキップして、該当ホスト名のレコードを逆順（最新から）で収集
        for (int i = lines.size() - 1; i >= 1 && result.size() < n; i--) {
            String line = lines.get(i);
            String[] parts = line.split(",", -1);
            if (parts.length >= 4 && parts[0].equals(hostname)) {
                ResourceInfo info = new ResourceInfo(
                        parts[0], // hostname
                        parts[1], // timestamp
                        Double.parseDouble(parts[2]), // cpuUsage
                        Double.parseDouble(parts[3]) // memoryUsage
                );
                result.add(info);
            }
        }

        return result;
    }
}
