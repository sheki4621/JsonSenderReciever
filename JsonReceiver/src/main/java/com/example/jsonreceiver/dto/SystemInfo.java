package com.example.jsonreceiver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * システム情報を保持するDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfo {
    /**
     * IPアドレス
     */
    private String ipAddress;

    /**
     * ホスト名
     */
    private String hostname;

    /**
     * EL種別
     */
    private String elType;

    /**
     * HEL名
     */
    private String helName;
}
