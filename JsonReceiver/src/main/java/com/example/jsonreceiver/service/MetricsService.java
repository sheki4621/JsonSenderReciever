package com.example.jsonreceiver.service;

import com.example.jsoncommon.dto.InstanceTypeChangeRequest;
import com.example.jsoncommon.dto.MetricsJson;
import com.example.jsoncommon.repository.ResourceHistoryRepository;
import com.example.jsonreceiver.dto.InstanceType;
import com.example.jsonreceiver.repository.InstanceStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ResourceHistoryRepository resourceHistoryRepository;
    private final InstanceStatusRepository instanceStatusRepository;
    private final InstanceTypeChangeService instanceTypeChangeService;
    // private final ThresholdService thresholdService;

    public void processMetrics(MetricsJson metricsJson) {
        try {
            // resource_history_{ホスト名}.csvに出力
            resourceHistoryRepository.save(metricsJson);

            // AGENT_LAST_NOTICE_TIMEを更新
            String currentTime = ZonedDateTime.now().format(TIMESTAMP_FORMATTER);
            instanceStatusRepository.updateAgentLastNoticeTime(metricsJson.getInstanceName(), currentTime);
            log.info("ホスト名 {} のAGENT_LAST_NOTICE_TIMEを更新しました: {}",
                    metricsJson.getInstanceName(), currentTime);

            // InstanceTypeChangeRequestをチェック
            InstanceTypeChangeRequest request = metricsJson.getMetrics().getInstanceTypeChangeRequest();
            if (request != null && request != InstanceTypeChangeRequest.WITHIN) {
                log.info("ホスト名 {} のINSTANCE_CHANGE_REQUESTを検出: {}",
                        metricsJson.getInstanceName(), request);

                InstanceType targetType = request == InstanceTypeChangeRequest.UPPER
                        ? InstanceType.HIGH
                        : InstanceType.LOW;

                instanceTypeChangeService.changeInstanceType(metricsJson.getInstanceName(), targetType);
            }

            // しきい値判定処理を実行
            // thresholdService.checkThreshold(metricsJson);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save metrics to CSV", e);
        }
    }
}
