package com.example.jsonreceiver.repository;

import com.example.jsonreceiver.dto.ThresholdInfo;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Repository
public class ThresholdRepository extends CsvRepositoryBase {

    private static final String FILE_NAME = "Threshold.csv";

    /**
     * ホスト名でしきい値情報を検索する
     * 
     * @param hostname ホスト名
     * @return しきい値情報（存在しない場合はOptional.empty()）
     * @throws IOException IO例外
     */
    public Optional<ThresholdInfo> findByHostname(String hostname) throws IOException {
        List<String> lines = readFromCsv(FILE_NAME);

        if (lines.isEmpty()) {
            return Optional.empty();
        }

        // ヘッダーをスキップして検索
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split(",", -1);
            if (parts.length >= 6 && parts[0].equals(hostname)) {
                ThresholdInfo threshold = new ThresholdInfo(
                        parts[0], // hostname
                        Double.parseDouble(parts[1]), // cpuUpperLimit
                        Double.parseDouble(parts[2]), // cpuLowerLimit
                        Double.parseDouble(parts[3]), // memoryUpperLimit
                        Double.parseDouble(parts[4]), // memoryLowerLimit
                        Integer.parseInt(parts[5]) // continueCount
                );
                return Optional.of(threshold);
            }
        }

        return Optional.empty();
    }
}
