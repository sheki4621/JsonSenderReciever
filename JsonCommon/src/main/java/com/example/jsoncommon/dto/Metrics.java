package com.example.jsoncommon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Metrics {
    @JsonProperty("CpuUsage")
    private Double cpuUsage;

    @JsonProperty("MemoryUsage")
    private Double memoryUsage;

    @JsonProperty("InstanceTypeChangeRequest")
    private InstanceTypeChangeRequest instanceTypeChangeRequest;
}
