package com.example.jsonreceiver.repository;

import org.springframework.stereotype.Repository;

import com.example.jsoncommon.dto.ConditionLogic;
import com.example.jsoncommon.dto.ScalingMode;
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
                "scalingMode",
                "upperChangeableEnable",
                "upperCpuThreshold",
                "upperCpuDurationMin",
                "upperMemThreshold",
                "upperMemDurationMin",
                "upperConditionLogic",
                "lowerChangeableEnable",
                "lowerCpuThreshold",
                "lowerCpuDurationMin",
                "lowerMemThreshold",
                "lowerMemDurationMin",
                "lowerConditionLogic",
                "microChangeableEnable",
                "microForceOnStandby"
        };
        writeToCsv(FILE_NAME, header,
                thresholdInfo.getHostname(),
                thresholdInfo.getScalingMode(),
                thresholdInfo.getUpperChangeableEnable(),
                thresholdInfo.getUpperCpuThreshold(),
                thresholdInfo.getUpperCpuDurationMin(),
                thresholdInfo.getUpperMemThreshold(),
                thresholdInfo.getUpperMemDurationMin(),
                thresholdInfo.getUpperConditionLogic(),
                thresholdInfo.getLowerChangeableEnable(),
                thresholdInfo.getLowerCpuThreshold(),
                thresholdInfo.getLowerCpuDurationMin(),
                thresholdInfo.getLowerMemThreshold(),
                thresholdInfo.getLowerMemDurationMin(),
                thresholdInfo.getLowerConditionLogic(),
                thresholdInfo.getMicroChangeableEnable(),
                thresholdInfo.getMicroForceOnStandby());
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

        // ヘッダーをスキップして検索
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split(",", -1);
            if (parts.length >= 16 && parts[0].equals(hostname)) {
                ThresholdInfo threshold = new ThresholdInfo(
                        parts[0], // hostname
                        (parts[1] == null || parts[1].equals("null") || parts[1].isEmpty()) ? null
                                : ScalingMode.valueOf(parts[1]), // scalingMode
                        (parts[2] == null || parts[2].isEmpty()) ? null : Boolean.parseBoolean(parts[2]), // upperChangeableEnable
                        (parts[3] == null || parts[3].isEmpty()) ? null : Double.parseDouble(parts[3]), // upperCpuThreshold
                        (parts[4] == null || parts[4].isEmpty()) ? null : Integer.parseInt(parts[4]), // upperCpuDurationMin
                        (parts[5] == null || parts[5].isEmpty()) ? null : Double.parseDouble(parts[5]), // upperMemThreshold
                        (parts[6] == null || parts[6].isEmpty()) ? null : Integer.parseInt(parts[6]), // upperMemDurationMin
                        (parts[7] == null || parts[7].isEmpty()) ? null : ConditionLogic.valueOf(parts[7]), // upperConditionLogic
                        (parts[8] == null || parts[8].isEmpty()) ? null : Boolean.parseBoolean(parts[8]), // lowerChangeableEnable
                        (parts[9] == null || parts[9].isEmpty()) ? null : Double.parseDouble(parts[9]), // lowerCpuThreshold
                        (parts[10] == null || parts[10].isEmpty()) ? null : Integer.parseInt(parts[10]), // lowerCpuDurationMin
                        (parts[11] == null || parts[11].isEmpty()) ? null : Double.parseDouble(parts[11]), // lowerMemThreshold
                        (parts[12] == null || parts[12].isEmpty()) ? null : Integer.parseInt(parts[12]), // lowerMemDurationMin
                        (parts[13] == null || parts[13].isEmpty()) ? null : ConditionLogic.valueOf(parts[13]), // lowerConditionLogic
                        (parts[14] == null || parts[14].isEmpty()) ? null : Boolean.parseBoolean(parts[14]), // microChangeableEnable
                        (parts[15] == null || parts[15].isEmpty()) ? null : Boolean.parseBoolean(parts[15]) // microForceOnStandby
                );
                return Optional.of(threshold);
            }
        }

        return Optional.empty();
    }
}
