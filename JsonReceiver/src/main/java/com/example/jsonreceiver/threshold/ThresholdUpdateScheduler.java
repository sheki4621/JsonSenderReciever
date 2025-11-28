package com.example.jsonreceiver.threshold;

import com.example.jsoncommon.dto.ConditionLogic;
import com.example.jsoncommon.dto.ScalingMode;
import com.example.jsoncommon.dto.Threshold;
import com.example.jsonreceiver.monitortarget.InstanceStatusCsv;
import com.example.jsonreceiver.monitortarget.InstanceStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@Slf4j
@RequiredArgsConstructor
public class ThresholdUpdateScheduler {

    private final InstanceStatusRepository instanceStatusRepository;
    private final ThresholdChangeService thresholdChangeService;
    private final Random random = new Random();

    // 10秒ごとに実行
    @Scheduled(fixedRate = 10000)
    public void updateThresholds() {
        log.info("定期的なしきい値変更処理を開始します");
        try {
            List<InstanceStatusCsv> instances = instanceStatusRepository.findAll();
            for (InstanceStatusCsv instance : instances) {
                // 簡易的なしきい値生成 (ランダムに値を変動させる)
                Threshold threshold = generateRandomThresholdConfig(instance.getHostname());
                thresholdChangeService.sendThresholdUpdate(instance.getHostname(), threshold);
            }
        } catch (Exception e) {
            log.error("しきい値変更処理中にエラーが発生しました", e);
        }
    }

    private Threshold generateRandomThresholdConfig(String hostname) {
        // テスト用に値をランダムに生成
        double upperCpu = 50.0 + random.nextDouble() * 40.0; // 50-90
        double lowerCpu = 10.0 + random.nextDouble() * 20.0; // 10-30

        return new Threshold(
                hostname,
                ScalingMode.AUTO,
                true, // upperChangeableEnable
                upperCpu,
                5, // upperCpuDurationMin
                80.0, // upperMemThreshold
                5, // upperMemDurationMin
                ConditionLogic.OR, // upperConditionLogic
                true, // lowerChangeableEnable
                lowerCpu,
                5, // lowerCpuDurationMin
                20.0, // lowerMemThreshold
                5, // lowerMemDurationMin
                ConditionLogic.OR, // lowerConditionLogic
                true, // microChangeableEnable
                false // microForceOnStandby
        );
    }
}
