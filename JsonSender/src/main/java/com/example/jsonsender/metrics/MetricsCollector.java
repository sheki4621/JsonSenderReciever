package com.example.jsonsender.metrics;

import org.springframework.stereotype.Component;

import com.example.jsonsender.utils.collector.Collector;

@Component
public class MetricsCollector implements Collector<Metrics> {

    @Override
    public Metrics collect() {
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
