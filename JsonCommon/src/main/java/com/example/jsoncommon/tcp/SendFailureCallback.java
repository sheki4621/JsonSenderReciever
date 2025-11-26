package com.example.jsoncommon.tcp;

/**
 * TCP送信失敗時のコールバックインターフェース
 */
public interface SendFailureCallback {
    /**
     * 送信失敗時に呼び出されます
     * 
     * @param data 送信に失敗したデータ
     */
    void onSendFailure(Object data);
}
