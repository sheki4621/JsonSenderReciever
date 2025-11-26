package com.example.jsoncommon.tcp;

import com.example.jsoncommon.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * TCPサーバー実装
 * JSON形式のメッセージを受信し、MessageHandlerに処理を委譲します
 */
public class TcpServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);
    private final int port;
    private final MessageHandler messageHandler;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private int actualPort;

    public TcpServer(int port, MessageHandler messageHandler, ExecutorService executorService,
            ObjectMapper objectMapper) {
        this.port = port;
        this.messageHandler = messageHandler;
        this.executorService = executorService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            this.actualPort = serverSocket.getLocalPort();
            logger.info("TCPサーバーがポート {} で起動しました", actualPort);

            while (running) {
                try (Socket clientSocket = serverSocket.accept();
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(clientSocket.getInputStream()))) {

                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        logger.info("受信した生データ: {}", inputLine);
                        processMessage(inputLine);
                    }
                } catch (Exception e) {
                    if (running) {
                        logger.error("クライアント処理中にエラーが発生しました", e);
                    }
                }
            }
        } catch (Exception e) {
            if (running) {
                logger.error("サーバー起動中にエラーが発生しました", e);
            }
        } finally {
            closeServerSocket();
        }
    }

    /**
     * 受信したメッセージを解析して処理します
     */
    private void processMessage(String json) {
        executorService.submit(() -> {
            try {
                JsonNode jsonNode = objectMapper.readTree(json);
                if (!jsonNode.has("NoticeType")) {
                    logger.warn("NoticeTypeフィールドが存在しません: {}", json);
                    return;
                }

                String noticeTypeStr = jsonNode.get("NoticeType").asText();
                NoticeBaseJson message = parseMessage(jsonNode, noticeTypeStr);

                if (message != null) {
                    messageHandler.handleMessage(message);
                    logger.info("{} を処理しました: {}", noticeTypeStr, message.getId());
                } else {
                    logger.warn("無視された NoticeType: {}", noticeTypeStr);
                }
            } catch (Exception e) {
                logger.error("JSON の解析または処理に失敗しました", e);
            }
        });
    }

    /**
     * NoticeTypeに応じてメッセージをパースします
     */
    private NoticeBaseJson parseMessage(JsonNode jsonNode, String noticeTypeStr) throws Exception {
        return switch (noticeTypeStr) {
            case "METRICS" -> objectMapper.treeToValue(jsonNode, MetricsJson.class);
            case "INSTALL" -> objectMapper.treeToValue(jsonNode, InstallJson.class);
            case "UNINSTALL" -> objectMapper.treeToValue(jsonNode, UninstallJson.class);
            case "UP" -> objectMapper.treeToValue(jsonNode, UpJson.class);
            case "DOWN" -> objectMapper.treeToValue(jsonNode, DownJson.class);
            default -> null;
        };
    }

    /**
     * サーバーを停止します
     */
    public void shutdown() {
        logger.info("TCPサーバーを停止しています");
        running = false;
        closeServerSocket();
    }

    /**
     * ServerSocketをクローズします
     */
    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                logger.info("ServerSocket が正常に閉じられました");
            } catch (Exception e) {
                logger.error("ServerSocket のクローズ中にエラーが発生しました", e);
            }
        }
    }

    /**
     * 実際にバインドされたポート番号を取得します
     */
    public int getPort() {
        return actualPort;
    }
}
