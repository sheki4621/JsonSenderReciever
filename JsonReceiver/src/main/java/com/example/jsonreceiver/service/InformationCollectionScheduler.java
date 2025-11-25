package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.InstanceTypeInfo;
import com.example.jsonreceiver.dto.SystemInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 情報収集スケジューラー
 * JsonReceiver起動後に別スレッドで定期的に情報収集を実行します。
 */
@Component
@RequiredArgsConstructor
public class InformationCollectionScheduler implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(InformationCollectionScheduler.class);

    private final InformationCollectionService informationCollectionService;

    @Value("${info.collection.interval-seconds:60}")
    private int collectionIntervalSeconds;

    @Override
    public void run(String... args) throws Exception {
        // 別スレッドで情報収集を開始
        Thread collectionThread = new Thread(() -> {
            logger.info("情報収集スレッドを開始しました（収集間隔: {}秒）", collectionIntervalSeconds);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // インスタンスタイプ一覧の収集
                    collectAndLogInstanceTypes();

                    // システム情報の収集
                    collectAndLogSystemInfo();

                    // 次の収集まで待機
                    logger.debug("次の収集まで{}秒待機します", collectionIntervalSeconds);
                    Thread.sleep(collectionIntervalSeconds * 1000L);

                } catch (InterruptedException e) {
                    logger.info("情報収集スレッドが中断されました");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // 予期しないエラーが発生してもスレッドを継続
                    logger.error("情報収集中に予期しないエラーが発生しました。次の収集まで待機します。", e);
                    try {
                        Thread.sleep(collectionIntervalSeconds * 1000L);
                    } catch (InterruptedException ie) {
                        logger.info("情報収集スレッドが中断されました");
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            logger.info("情報収集スレッドを終了しました");
        }, "InfoCollectionThread");

        // デーモンスレッドとして起動（アプリケーション終了時に自動的に終了）
        collectionThread.setDaemon(true);
        collectionThread.start();
    }

    /**
     * インスタンスタイプ一覧を収集してログ出力
     */
    private void collectAndLogInstanceTypes() {
        try {
            logger.info("=== インスタンスタイプ一覧の収集を開始 ===");
            List<InstanceTypeInfo> instanceTypes = informationCollectionService.collectInstanceTypes();
            logger.info("インスタンスタイプを{}件取得しました", instanceTypes.size());

            for (InstanceTypeInfo info : instanceTypes) {
                logger.info("  - インスタンスタイプ: {}", info.getInstanceType());
            }

        } catch (Exception e) {
            // リトライが全て失敗した場合、エラーログを出力して次の収集まで待機
            logger.error("インスタンスタイプ一覧の収集に失敗しました。次の収集まで待機します。", e);
        }
    }

    /**
     * システム情報を収集してログ出力
     */
    private void collectAndLogSystemInfo() {
        try {
            logger.info("=== システム情報の収集を開始 ===");
            List<SystemInfo> systemInfoList = informationCollectionService.collectSystemInfo();
            logger.info("システム情報を{}件取得しました", systemInfoList.size());

            for (SystemInfo info : systemInfoList) {
                logger.info("  - IPアドレス: {}, ホスト名: {}, EL種別: {}, HEL名: {}",
                        info.getIpAddress(), info.getHostname(), info.getElType(), info.getHelName());
            }

        } catch (Exception e) {
            // リトライが全て失敗した場合、エラーログを出力して次の収集まで待機
            logger.error("システム情報の収集に失敗しました。次の収集まで待機します。", e);
        }
    }
}
