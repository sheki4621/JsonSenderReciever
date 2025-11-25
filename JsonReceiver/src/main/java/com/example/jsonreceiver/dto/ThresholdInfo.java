package com.example.jsonreceiver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdInfo {
    private String hostname;
    private Double cpuUpperLimit;
    private Double cpuLowerLimit;
    private Double memoryUpperLimit;
    private Double memoryLowerLimit;
    private Integer continueCount;
}
