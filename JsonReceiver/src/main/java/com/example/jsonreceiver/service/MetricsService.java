package com.example.jsonreceiver.service;

import com.example.jsoncommon.dto.MetricsJson;
import com.example.jsonreceiver.repository.CsvRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final CsvRepository csvRepository;
    private final ThresholdService thresholdService;

    public void processMetrics(MetricsJson metricsJson) {
        try {
            // ResourceInfo.csvに出力
            csvRepository.saveResourceInfo(metricsJson);

            // しきい値判定処理を実行
            thresholdService.checkThreshold(metricsJson);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save metrics to CSV", e);
        }
    }
}
