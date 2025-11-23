package com.example.jsonsender.metrics;

import com.example.jsonsender.collector.Collector;
import org.springframework.stereotype.Component;

@Component
public class MetricsCollector implements Collector<Metrics> {

    @Override
    public Metrics collect() {
        double cpuUsage = getCpuUsage();
        double memoryUsage = getMemoryUsage();

        return new Metrics(cpuUsage, memoryUsage);
    }

    private double getCpuUsage() {
        // TODO: 提供されたら修正
        return 23.4;
    }

    private double getMemoryUsage() {
        // TODO: 提供されたら修正
        return 34.5;
    }
}
