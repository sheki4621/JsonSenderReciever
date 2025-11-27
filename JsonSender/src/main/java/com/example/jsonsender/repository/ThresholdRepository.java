package com.example.jsonsender.repository;

import org.springframework.stereotype.Repository;

import com.example.jsoncommon.dto.ConditionLogic;
import com.example.jsoncommon.dto.ThresholdInfo;
import com.example.jsoncommon.repository.CsvRepositoryBase;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Repository
public class ThresholdRepository extends CsvRepositoryBase {

    private static final String FILE_NAME = "threshold.csv";

    public void save(ThresholdInfo thresholdInfo) throws IOException {
        String[] header = {
                "hostname",
                "upperConditionLogic",
                "upperCpuThreshold",
                "upperMemThreshold",
                "upperCpuDurationMin",
                "upperMemDurationMin",
                "lowerConditionLogic",
                "lowerCpuThreshold",
                "lowerMemThreshold",
                "lowerCpuDurationMin",
                "lowerMemDurationMin",
                "upperChangeableEnable",
                "lowerChangeableEnable",
                "microChangeableEnable",
                "microForceOnStandby"
        };
        writeToCsv(FILE_NAME, header, thresholdInfo);
    }

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
        ConditionLogic.valueOf("OR");
        // ヘッダーをスキップして検索
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split(",", -1);
            if (parts.length >= 16 && parts[0].equals(hostname)) {
                ThresholdInfo threshold = new ThresholdInfo(
                        parts[0], // hostname
                        ConditionLogic.valueOf(parts[1]), // upperConditionLogic
                        Double.parseDouble(parts[2]), // upperCpuThreshold
                        Double.parseDouble(parts[3]), // upperMemThreshold
                        Integer.parseInt(parts[4]), // upperCpuDurationMin
                        Integer.parseInt(parts[5]), // upperMemDurationMin
                        ConditionLogic.valueOf(parts[6]), // lowerConditionLogic
                        Double.parseDouble(parts[7]), // lowerCpuThreshold
                        Double.parseDouble(parts[8]), // lowerMemThreshold
                        Integer.parseInt(parts[9]), // lowerCpuDurationMin
                        Integer.parseInt(parts[10]), // lowerMemDurationMin
                        Boolean.parseBoolean(parts[11]), // upperChangeableEnable
                        Boolean.parseBoolean(parts[12]), // lowerChangeableEnable
                        Boolean.parseBoolean(parts[13]), // microChangeableEnable
                        Boolean.parseBoolean(parts[14]) // microForceOnStandby
                );
                return Optional.of(threshold);
            }
        }

        return Optional.empty();
    }
}
