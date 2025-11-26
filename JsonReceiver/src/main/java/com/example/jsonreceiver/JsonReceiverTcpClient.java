package com.example.jsonreceiver;

import com.example.jsoncommon.dto.NoticeBaseJson;
import com.example.jsoncommon.tcp.SendFailureCallback;
import com.example.jsoncommon.tcp.TcpClient;
import com.example.jsoncommon.tcp.TcpConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JsonReceiverでJsonSenderへメッセージを送信するためのTCPクライアント
 */
@Component
@Slf4j
public class JsonReceiverTcpClient implements SendFailureCallback {

    private final TcpClient tcpClient;
    private final String targetHost;
    private final int targetPort;
    private final TcpConfig tcpConfig;

    public JsonReceiverTcpClient(
            ObjectMapper objectMapper,
            @Value("${tcp.client.target-host:localhost}") String targetHost,
            @Value("${tcp.client.target-port:8888}") int targetPort,
            @Value("${tcp.client.retry-max:3}") int retryMax,
            @Value("${tcp.client.retry-interval-sec:1}") int retryIntervalSec,
            @Value("${tcp.client.timeout:3}") int timeout) {

        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.tcpConfig = new TcpConfig(timeout, retryMax, retryIntervalSec);
        this.tcpClient = new TcpClient(objectMapper);

        log.info("JsonReceiverTcpClientを初期化しました: target={}:{}", targetHost, targetPort);
    }

    /**
     * JsonSenderにメッセージを送信します
     */
    public void sendMessage(NoticeBaseJson message) {
        log.info("JsonSenderにメッセージを送信します: type={}, instance={}",
                message.getNoticeType(), message.getInstanceName());
        boolean success = tcpClient.sendJsonWithCallback(targetHost, targetPort, message, tcpConfig, this);
        if (success) {
            log.info("メッセージ送信成功: type={}", message.getNoticeType());
        }
    }

    @Override
    public void onSendFailure(Object data) {
        log.error("JsonSenderへのメッセージ送信に失敗しました: {}", data);
        // 必要に応じて再送処理やエラー処理を追加
    }
}
