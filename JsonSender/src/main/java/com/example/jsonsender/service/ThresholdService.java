package com.example.jsonsender.service;

import com.example.jsoncommon.dto.Threshold;
import com.example.jsoncommon.dto.ThresholdCsv;
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
    public void updateThreshold(String hostname, Threshold threshold) {
        log.info("しきい値を更新します: hostname={}, config={}", hostname, threshold);

        try {
            ThresholdCsv thresholdInfo = new ThresholdCsv(
                    hostname,
                    threshold.getScalingMode(),
                    threshold.getUpperChangeableEnable(),
                    threshold.getUpperCpuThreshold(),
                    threshold.getUpperCpuDurationMin(),
                    threshold.getUpperMemThreshold(),
                    threshold.getUpperMemDurationMin(),
                    threshold.getUpperConditionLogic(),
                    threshold.getLowerChangeableEnable(),
                    threshold.getLowerCpuThreshold(),
                    threshold.getLowerCpuDurationMin(),
                    threshold.getLowerMemThreshold(),
                    threshold.getLowerMemDurationMin(),
                    threshold.getLowerConditionLogic(),
                    threshold.getMicroChangeableEnable(),
                    threshold.getMicroForceOnStandby());

            thresholdRepository.save(thresholdInfo);
            log.info("しきい値を保存しました");
        } catch (IOException e) {
            log.error("しきい値の保存に失敗しました", e);
        }
    }
}
