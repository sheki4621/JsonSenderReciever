package com.example.jsonreceiver.instancetype;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * システム情報を保持するDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllInstanceCsv {
    /**
     * ホスト名
     */
    private String hostname;

    /**
     * 装置タイプ(ECS, EDB...)
     */
    private String machineType;

    /**
     * グループ名
     */
    private String groupName;
}
