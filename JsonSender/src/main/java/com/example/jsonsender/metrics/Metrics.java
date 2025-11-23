package com.example.jsonsender.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Metrics {
    @JsonProperty("CpuUsage")
    private Double cpuUsage;

    @JsonProperty("MemoryUsage")
    private Double memoryUsage;

    public Metrics() {
    }

    public Metrics(Double cpuUsage, Double memoryUsage) {
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(Double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }
}
