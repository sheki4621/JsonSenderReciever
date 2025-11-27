package com.example.jsonsender.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.example.jsoncommon.dto.ConditionLogic;
import com.example.jsoncommon.dto.ScalingMode;
import com.example.jsoncommon.dto.ThresholdInfo;
import com.example.jsoncommon.repository.CsvRepositoryBase;
import com.example.jsonsender.utils.HostnameUtil;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Repository
public class ThresholdRepository extends CsvRepositoryBase {

    private static final Logger logger = LoggerFactory.getLogger(ThresholdRepository.class);

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
        writeToCsv(getFilePath(), header,
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
        try {
            List<String> lines = readFromCsv(getFilePath());

            if (lines.isEmpty()) {
                return Optional.empty();
            }

            // ヘッダーをスキップして検索
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] parts = line.split(",", -1);
                if (parts.length >= 16 && parts[0].equals(hostname)) { // Changed from 17 to 16 based on save method's
                                                                       // header length
                    ThresholdInfo threshold = new ThresholdInfo(
                            parts[0], // hostname
                            (parts[1] == null || parts[1].equals("null") || parts[1].isEmpty()) ? null
                                    : ScalingMode.valueOf(parts[1]), // scalingMode
                            Boolean.parseBoolean(parts[2]), // upperChangeableEnable
                            Double.parseDouble(parts[3]), // upperCpuThreshold
                            Integer.parseInt(parts[4]), // upperCpuDurationMin
                            Double.parseDouble(parts[5]), // upperMemThreshold
                            Integer.parseInt(parts[6]), // upperMemDurationMin
                            ConditionLogic.valueOf(parts[7]), // upperConditionLogic
                            Boolean.parseBoolean(parts[8]), // lowerChangeableEnable
                            Double.parseDouble(parts[9]), // lowerCpuThreshold
                            Integer.parseInt(parts[10]), // lowerCpuDurationMin
                            Double.parseDouble(parts[11]), // lowerMemThreshold
                            Integer.parseInt(parts[12]), // lowerMemDurationMin
                            ConditionLogic.valueOf(parts[13]), // lowerConditionLogic
                            Boolean.parseBoolean(parts[14]), // microChangeableEnable
                            Boolean.parseBoolean(parts[15]) // microForceOnStandby
                    );
                    return Optional.of(threshold);
                }
            }

            return Optional.empty();
        } catch (IOException e) {
            logger.error("しきい値情報の取得に失敗しました");
            return Optional.empty();
        }
    }

    public String getFilePath() {
        return String.format("threshold_{}.csv", HostnameUtil.getHostname());
    }
}
