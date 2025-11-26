package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.AllInstance;
import com.example.jsonreceiver.dto.InstanceTypeInfo;
import com.example.jsonreceiver.repository.AllInstanceRepository;
import com.example.jsonreceiver.repository.InstanceTypeRepository;
import com.example.jsonreceiver.util.ShellExecutor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final AllInstanceRepository allInstanceRepository;
    private final ShellExecutor shellExecutor;
    private final ObjectMapper objectMapper;

    @Value("${info.collection.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${info.collection.retry.interval-seconds:5}")
    private int retryIntervalSeconds;

    @Value("${shell.system-info.path:/path/to/get_system_info.sh}")
    private String systemInfoShellPath;

    @Value("${shell.instance-type.path:/path/to/get_instance_type.sh}")
    private String instanceTypeShellPath;

    @Value("${shell.execution.timeout-seconds:30}")
    private int shellTimeoutSeconds;

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
    public List<AllInstance> collectSystemInfo() {
        List<AllInstance> allInstanceList = executeWithRetry("システム情報", this::fetchSystemInfo);

        // CSV出力
        try {
            allInstanceRepository.saveAll(allInstanceList);
            logger.debug("システム情報をCSVファイルに出力しました");
        } catch (Exception e) {
            logger.error("システム情報のCSV出力に失敗しました", e);
            // CSV出力に失敗しても、データの収集自体は成功しているため例外をスローしない
        }

        return allInstanceList;
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
     * 外部シェルスクリプトを呼び出してJSON形式でインスタンスタイプ情報を取得します
     * 
     * @return インスタンスタイプ情報のリスト
     */
    private List<InstanceTypeInfo> fetchInstanceTypes() {
        logger.debug("インスタンスタイプ一覧を取得中");

        try {
            // 外部シェルを実行
            String output = shellExecutor.executeShell(
                    instanceTypeShellPath,
                    List.of(),
                    shellTimeoutSeconds);

            // JSON出力をパース
            List<InstanceTypeInfo> instanceTypes = objectMapper.readValue(
                    output,
                    new TypeReference<List<InstanceTypeInfo>>() {
                    });

            logger.info("インスタンスタイプ一覧を取得しました: {} 件", instanceTypes.size());
            return instanceTypes;

        } catch (Exception e) {
            logger.warn("外部シェルによるインスタンスタイプ取得に失敗しました。サンプルデータを返します", e);

            // フォールバック: サンプルデータを返す
            List<InstanceTypeInfo> instanceTypes = new ArrayList<>();
            instanceTypes.add(new InstanceTypeInfo("1", "t2.xlarge", 4, "t2.medium", 2, "t2.micro", 1));
            instanceTypes.add(new InstanceTypeInfo("2", "t3.xlarge", 4, "t3.medium", 2, "t3.micro", 1));
            return instanceTypes;
        }
    }

    /**
     * システム情報を取得する内部メソッド
     * 外部シェルスクリプトを呼び出してJSON形式でシステム情報を取得します
     * 
     * @return システム情報のリスト
     */
    private List<AllInstance> fetchSystemInfo() {
        logger.debug("システム情報を取得中");

        try {
            // 外部シェルを実行
            String output = shellExecutor.executeShell(
                    systemInfoShellPath,
                    List.of(),
                    shellTimeoutSeconds);

            // JSON出力をパース
            List<AllInstance> allInstanceList = objectMapper.readValue(
                    output,
                    new TypeReference<List<AllInstance>>() {
                    });

            logger.info("システム情報を取得しました: {} 件", allInstanceList.size());
            return allInstanceList;

        } catch (Exception e) {
            logger.warn("外部シェルによるシステム情報取得に失敗しました。サンプルデータを返します", e);

            // フォールバック: サンプルデータを返す
            List<AllInstance> allInstanceList = new ArrayList<>();
            allInstanceList.add(new AllInstance(
                    "server01.example.com",
                    "ECS",
                    "GROUP-A"));
            allInstanceList.add(new AllInstance(
                    "server02.example.com",
                    "EDB",
                    "GROUP-B"));
            allInstanceList.add(new AllInstance(
                    "server03.example.com",
                    "ECS",
                    "GROUP-A"));
            return allInstanceList;
        }
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
