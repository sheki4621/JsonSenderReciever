package com.example.jsoncommon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceHistory {
    private String hostname;
    private String timestamp;
    private Double cpuUsage;
    private Double memoryUsage;
    private InstanceTypeChangeRequest instanceTypeChangeRequest;
}
