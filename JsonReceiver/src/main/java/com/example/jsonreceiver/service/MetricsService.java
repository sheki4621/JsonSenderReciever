package com.example.jsonreceiver.service;

import com.example.jsoncommon.dto.MetricsJson;
import com.example.jsoncommon.repository.ResourceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final ResourceHistoryRepository resourceHistoryRepository;
    // private final ThresholdService thresholdService;

    public void processMetrics(MetricsJson metricsJson) {
        try {
            // ResourceInfo.csvに出力
            resourceHistoryRepository.save(metricsJson);

            // しきい値判定処理を実行
            // thresholdService.checkThreshold(metricsJson);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save metrics to CSV", e);
        }
    }
}
