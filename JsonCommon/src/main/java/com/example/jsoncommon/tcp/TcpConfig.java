package com.example.jsoncommon.tcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TCP接続設定
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TcpConfig {
    /**
     * 接続タイムアウト（秒）
     */
    @Builder.Default
    private int timeout = 5;

    /**
     * リトライ最大回数
     */
    @Builder.Default
    private int retryMax = 3;

    /**
     * リトライ間隔（秒）
     */
    @Builder.Default
    private int retryIntervalSec = 1;
}
