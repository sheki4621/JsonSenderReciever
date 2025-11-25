package com.example.jsonreceiver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstanceStatus {
    private String hostname;
    private InstanceStatusValue status;
    private Boolean isInstalled;
    private String agentVersion;
    private String timestamp;
    private InstanceType instanceType;
}
