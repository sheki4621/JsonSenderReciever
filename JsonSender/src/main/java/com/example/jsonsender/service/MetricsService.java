package com.example.jsonsender.service;

import com.example.jsoncommon.dto.InstanceTypeChangeRequest;
import com.example.jsoncommon.dto.Metrics;
import com.example.jsonsender.utils.collector.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MetricsService implements Collector<Metrics> {

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

    private final ThresholdService thresholdService;

    public MetricsService(ThresholdService thresholdService) {
        this.thresholdService = thresholdService;
    }

    @Override
    public Metrics collect() throws Exception {
        Double cpuUsage = getCpuUsage();
        Double memoryUsage = getMemoryUsage();

        InstanceTypeChangeRequest instanceTypeChangeRequest;
        try {
            instanceTypeChangeRequest = thresholdService.checkThreshold(cpuUsage, memoryUsage);
        } catch (Exception e) {
            logger.error("しきい値チェック中にエラーが発生しました: " + e.getMessage());
            instanceTypeChangeRequest = null;
        }

        return new Metrics(cpuUsage, memoryUsage, instanceTypeChangeRequest);
    }

    protected Double getCpuUsage() {
        // TODO: 提供されたら修正
        return 23.4;
    }

    protected Double getMemoryUsage() {
        // TODO: 提供されたら修正
        return 34.5;
    }
}
