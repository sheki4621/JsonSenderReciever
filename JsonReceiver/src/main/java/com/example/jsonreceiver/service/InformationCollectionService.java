package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.InstanceTypeInfo;
import com.example.jsonreceiver.dto.SystemInfo;
import com.example.jsonreceiver.repository.InstanceTypeRepository;
import com.example.jsonreceiver.repository.SystemInfoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 情報収集サービス
 * インスタンスタイプとシステム情報を定期的に収集します。
 * 現在はサンプルデータを返し、将来的にシェルスクリプトを呼び出す構造になっています。
 */
@Service
@RequiredArgsConstructor
public class InformationCollectionService {

    private static final Logger logger = LoggerFactory.getLogger(InformationCollectionService.class);

    private final InstanceTypeRepository instanceTypeRepository;
    private final SystemInfoRepository systemInfoRepository;

    @Value("${info.collection.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${info.collection.retry.interval-seconds:5}")
    private int retryIntervalSeconds;

    /**
     * インスタンスタイプ一覧を収集します。
     * リトライロジック付きで、失敗時は設定された回数まで再試行します。
     * 収集したデータはCSVファイルに上書き出力されます。
     * 
     * @return インスタンスタイプ情報のリスト
     * @throws RuntimeException すべてのリトライが失敗した場合
     */
    public List<InstanceTypeInfo> collectInstanceTypes() {
        List<InstanceTypeInfo> instanceTypes = executeWithRetry("インスタンスタイプ一覧", this::fetchInstanceTypes);

        // CSV出力
        try {
            instanceTypeRepository.saveAll(instanceTypes);
            logger.debug("インスタンスタイプ一覧をCSVファイルに出力しました");
        } catch (Exception e) {
            logger.error("インスタンスタイプのCSV出力に失敗しました", e);
            // CSV出力に失敗しても、データの収集自体は成功しているため例外をスローしない
        }

        return instanceTypes;
    }

    /**
     * システム情報を収集します。
     * リトライロジック付きで、失敗時は設定された回数まで再試行します。
     * 収集したデータはCSVファイルに上書き出力されます。
     * 
     * @return システム情報のリスト
     * @throws RuntimeException すべてのリトライが失敗した場合
     */
    public List<SystemInfo> collectSystemInfo() {
        List<SystemInfo> systemInfoList = executeWithRetry("システム情報", this::fetchSystemInfo);

        // CSV出力
        try {
            systemInfoRepository.saveAll(systemInfoList);
            logger.debug("システム情報をCSVファイルに出力しました");
        } catch (Exception e) {
            logger.error("システム情報のCSV出力に失敗しました", e);
            // CSV出力に失敗しても、データの収集自体は成功しているため例外をスローしない
        }

        return systemInfoList;
    }

    /**
     * リトライロジックを実装した汎用実行メソッド
     * 
     * @param <T>           戻り値の型
     * @param operationName 操作名（ログ出力用）
     * @param operation     実行する操作
     * @return 操作の結果
     * @throws RuntimeException すべてのリトライが失敗した場合
     */
    private <T> T executeWithRetry(String operationName, CollectionOperation<T> operation) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetryAttempts) {
            try {
                attempt++;
                logger.debug("{}取得を実行中 (試行回数: {}/{})", operationName, attempt, maxRetryAttempts);
                T result = operation.execute();
                if (attempt > 1) {
                    logger.info("{}取得が成功しました (試行回数: {})", operationName, attempt);
                }
                return result;
            } catch (Exception e) {
                lastException = e;
                logger.warn("{}取得が失敗しました (試行回数: {}/{}): {}",
                        operationName, attempt, maxRetryAttempts, e.getMessage());

                if (attempt < maxRetryAttempts) {
                    try {
                        logger.debug("{}秒後に再試行します", retryIntervalSeconds);
                        Thread.sleep(retryIntervalSeconds * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(operationName + "取得中に中断されました", ie);
                    }
                }
            }
        }

        // すべてのリトライが失敗した場合
        logger.error("{}取得が{}回の試行後も失敗しました", operationName, maxRetryAttempts);
        throw new RuntimeException(operationName + "取得に失敗しました", lastException);
    }

    /**
     * インスタンスタイプ一覧を取得する内部メソッド
     * 将来的にシェルスクリプトを呼び出す処理に置き換える想定
     * 
     * @return インスタンスタイプ情報のリスト
     */
    private List<InstanceTypeInfo> fetchInstanceTypes() {
        logger.debug("インスタンスタイプ一覧を取得中（サンプルデータ）");

        // TODO: 将来的にシェルスクリプトを呼び出す処理に置き換える
        List<InstanceTypeInfo> instanceTypes = new ArrayList<>();
        instanceTypes.add(new InstanceTypeInfo("1", "t2.micro"));
        instanceTypes.add(new InstanceTypeInfo("2", "t2.small"));
        instanceTypes.add(new InstanceTypeInfo("3", "t2.medium"));
        instanceTypes.add(new InstanceTypeInfo("4", "t3.micro"));
        instanceTypes.add(new InstanceTypeInfo("5", "t3.small"));

        return instanceTypes;
    }

    /**
     * システム情報を取得する内部メソッド
     * 将来的にシェルスクリプトを呼び出す処理に置き換える想定
     * 
     * @return システム情報のリスト
     */
    private List<SystemInfo> fetchSystemInfo() {
        logger.debug("システム情報を取得中（サンプルデータ）");

        // TODO: 将来的にシェルスクリプトを呼び出す処理に置き換える
        List<SystemInfo> systemInfoList = new ArrayList<>();
        systemInfoList.add(new SystemInfo(
                "192.168.1.10",
                "server01.example.com",
                "RHEL",
                "HEL-PROD-01"));
        systemInfoList.add(new SystemInfo(
                "192.168.1.20",
                "server02.example.com",
                "CentOS",
                "HEL-PROD-02"));
        systemInfoList.add(new SystemInfo(
                "192.168.1.30",
                "server03.example.com",
                "Ubuntu",
                "HEL-DEV-01"));

        return systemInfoList;
    }

    /**
     * 収集操作を表す関数型インターフェース
     * 
     * @param <T> 戻り値の型
     */
    @FunctionalInterface
    private interface CollectionOperation<T> {
        T execute() throws Exception;
    }
}
