package com.example.jsoncommon.tcp;

import com.example.jsoncommon.dto.NoticeBaseJson;

/**
 * TCPサーバーが受信したメッセージを処理するためのインターフェース
 */
public interface MessageHandler {
    /**
     * 受信したメッセージを処理します
     * 
     * @param message 受信したメッセージ
     */
    void handleMessage(NoticeBaseJson message);
}
