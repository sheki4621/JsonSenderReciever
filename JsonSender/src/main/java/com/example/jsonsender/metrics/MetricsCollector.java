package com.example.jsonsender.metrics;

import com.example.jsoncommon.dto.Metrics;
import com.example.jsonsender.utils.collector.Collector;
import org.springframework.stereotype.Component;

@Component
public class MetricsCollector implements Collector<Metrics> {

    @Override
    public Metrics collect() throws Exception {
        Double cpuUsage = getCpuUsage();
        Double memoryUsage = getMemoryUsage();

        return new Metrics(cpuUsage, memoryUsage);
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
