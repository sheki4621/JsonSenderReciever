package com.example.jsoncommon.tcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.net.Socket;

/**
 * TCPクライアント実装
 * JSON形式のデータをTCP経由で送信します
 */
public class TcpClient {

    private static final Logger logger = LoggerFactory.getLogger(TcpClient.class);
    private final ObjectMapper objectMapper;

    public TcpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * JSONデータを送信します
     * 
     * @param host   送信先ホスト
     * @param port   送信先ポート
     * @param data   送信データ
     * @param config TCP接続設定
     * @return 送信成功の場合true
     */
    public boolean sendJson(String host, int port, Object data, TcpConfig config) {
        int retryMax = config.getRetryMax();
        int retryIntervalSec = config.getRetryIntervalSec();
        int timeout = config.getTimeout();

        for (int i = 0; i <= retryMax; i++) {
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), timeout * 1000);
                try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                    String json = objectMapper.writeValueAsString(data);
                    logger.info("JSONを送信します: {}", json);
                    out.println(json);
                    return true; // Success
                }
            } catch (Exception e) {
                logger.warn("JSON送信エラー (試行 {}/{}): {}", i + 1, retryMax + 1, e.getMessage());
                if (i < retryMax) {
                    try {
                        Thread.sleep(retryIntervalSec * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        logger.error("{}回の試行後もJSON送信に失敗しました", retryMax + 1);
        return false;
    }

    /**
     * JSONデータを送信します（失敗時にコールバックを呼び出し）
     * 
     * @param host            送信先ホスト
     * @param port            送信先ポート
     * @param data            送信データ
     * @param config          TCP接続設定
     * @param failureCallback 送信失敗時のコールバック（nullの場合は呼び出されません）
     * @return 送信成功の場合true
     */
    public boolean sendJsonWithCallback(String host, int port, Object data, TcpConfig config,
            SendFailureCallback failureCallback) {
        boolean success = sendJson(host, port, data, config);
        if (!success && failureCallback != null) {
            failureCallback.onSendFailure(data);
        }
        return success;
    }
}
