package com.example.jsonreceiver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceInfo {
    private String hostname;
    private String timestamp;
    private Double cpuUsage;
    private Double memoryUsage;
}
