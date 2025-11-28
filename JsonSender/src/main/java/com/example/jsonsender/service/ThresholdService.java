package com.example.jsonsender.service;

import com.example.jsoncommon.dto.ThresholdConfig;
import com.example.jsoncommon.dto.ThresholdInfo;
import com.example.jsonsender.repository.ThresholdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ThresholdService {

    private final ThresholdRepository thresholdRepository;

    /**
     * しきい値を更新します
     *
     * @param hostname ホスト名
     * @param config   しきい値設定
     */
    public void updateThreshold(String hostname, ThresholdConfig config) {
        log.info("しきい値を更新します: hostname={}, config={}", hostname, config);

        try {
            ThresholdInfo thresholdInfo = new ThresholdInfo(
                    hostname,
                    config.getScalingMode(),
                    config.getUpperChangeableEnable(),
                    config.getUpperCpuThreshold(),
                    config.getUpperCpuDurationMin(),
                    config.getUpperMemThreshold(),
                    config.getUpperMemDurationMin(),
                    config.getUpperConditionLogic(),
                    config.getLowerChangeableEnable(),
                    config.getLowerCpuThreshold(),
                    config.getLowerCpuDurationMin(),
                    config.getLowerMemThreshold(),
                    config.getLowerMemDurationMin(),
                    config.getLowerConditionLogic(),
                    config.getMicroChangeableEnable(),
                    config.getMicroForceOnStandby());

            thresholdRepository.save(thresholdInfo);
            log.info("しきい値を保存しました");
        } catch (IOException e) {
            log.error("しきい値の保存に失敗しました", e);
        }
    }
}
