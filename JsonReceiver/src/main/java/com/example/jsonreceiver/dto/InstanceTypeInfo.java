package com.example.jsonreceiver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * インスタンスタイプ情報を保持するDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstanceTypeInfo {
    /**
     * ID
     */
    private String id;

    /**
     * インスタンスタイプ名
     */
    private String instanceType;
}
