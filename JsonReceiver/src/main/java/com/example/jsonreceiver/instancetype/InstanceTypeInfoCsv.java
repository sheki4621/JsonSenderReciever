package com.example.jsonreceiver.instancetype;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * インスタンスタイプ情報を保持するDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstanceTypeInfoCsv {
    /**
     * インスタンスタイプID
     */
    private String instanceTypeId;

    /**
     * Highインスタンスタイプ
     */
    private String highInstanceType;

    /**
     * HighCPUコア数
     */
    private Integer highCpuCore;

    /**
     * Lowインスタンスタイプ
     */
    private String lowInstanceType;

    /**
     * LowCPUコア数
     */
    private Integer lowCpuCore;

    /**
     * VeryLowインスタンスタイプ
     */
    private String veryLowInstanceType;

    /**
     * VeryLowCPUコア数
     */
    private Integer veryLowCpuCore;
}
