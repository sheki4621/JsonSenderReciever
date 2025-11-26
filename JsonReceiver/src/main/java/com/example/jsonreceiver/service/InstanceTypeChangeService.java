package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.*;
import com.example.jsonreceiver.repository.*;
import com.example.jsonreceiver.util.ShellExecutor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * インスタンスタイプ変更サービス
 * インスタンスタイプの変更処理と変更確認スレッドを管理します
 */
@Service
@RequiredArgsConstructor
public class InstanceTypeChangeService {

    private static final Logger logger = LoggerFactory.getLogger(InstanceTypeChangeService.class);

    private final InstanceStatusRepository instanceStatusRepository;
    private final SystemInfoRepository systemInfoRepository;
    private final InstanceTypeLinkRepository instanceTypeLinkRepository;
    private final InstanceTypeRepository instanceTypeRepository;
    private final ShellExecutor shellExecutor;

    private final ScheduledExecutorService monitoringExecutor = Executors.newScheduledThreadPool(5);

    @Value("${instance-type-change.check-interval-seconds:5}")
    private int checkIntervalSeconds;

    @Value("${instance-type-change.max-retry-count:10}")
    private int maxRetryCount;

    @Value("${shell.instance-type-change.execute.path:/path/to/change_instance_type.sh}")
    private String executeInstanceTypeChangeShellPath;

    @Value("${shell.instance-type-change.check.path:/path/to/check_instance_type_change.sh}")
    private String checkInstanceTypeChangeShellPath;

    @Value("${shell.execution.timeout-seconds:30}")
    private int shellTimeoutSeconds;

    /**
     * インスタンスタイプを変更します
     * SystemInfo.csv → InstanceTypeLink.csv → InstanceType.csvからインスタンスタイプを取得し、
     * 外部シェルを呼び出してインスタンスタイプを変更します（外部呼び出しは空実装）
     * 
     * @param hostname           ホスト名
     * @param targetInstanceType 変更先のインスタンスタイプ
     */
    public void changeInstanceType(String hostname, InstanceType targetInstanceType) {
        logger.info("ホスト名 {} のインスタンスタイプを {} に変更します", hostname, targetInstanceType);

        try {
            // 1. SystemInfo.csvからホスト名でElTypeを取得
            Optional<SystemInfo> systemInfoOpt = systemInfoRepository.findByHostname(hostname);
            if (systemInfoOpt.isEmpty()) {
                logger.error("ホスト名 {} の SystemInfo が見つかりません", hostname);
                return;
            }
            SystemInfo systemInfo = systemInfoOpt.get();
            String elType = systemInfo.getElType();
            logger.debug("ホスト名 {} の ElType を検出: {}", hostname, elType);

            // 2. InstanceTypeLink.csvからElTypeでInstanceTypeIdを取得
            Optional<InstanceTypeLink> linkOpt = instanceTypeLinkRepository.findByElType(elType);
            if (linkOpt.isEmpty()) {
                logger.error("ElType {} に対する InstanceTypeLink が見つかりません", elType);
                return;
            }
            InstanceTypeLink link = linkOpt.get();
            String instanceTypeId = link.getInstanceTypeId();
            logger.debug("ElType {} の InstanceTypeId を検出: {}", elType, instanceTypeId);

            // 3. InstanceType.csvからInstanceTypeIdで対応するインスタンスタイプを取得
            Optional<InstanceTypeInfo> typeInfoOpt = instanceTypeRepository.findByInstanceTypeId(instanceTypeId);
            if (typeInfoOpt.isEmpty()) {
                logger.error("InstanceTypeId {} に対する InstanceType が見つかりません", instanceTypeId);
                return;
            }
            InstanceTypeInfo typeInfo = typeInfoOpt.get();

            // 4. targetInstanceTypeに応じて適切なインスタンスタイプを選択
            String actualInstanceType;
            if (targetInstanceType == InstanceType.HIGH) {
                actualInstanceType = typeInfo.getHighInstanceType();
                logger.info("選択された HIGH インスタンスタイプ: {} (CPU コア数: {})",
                        actualInstanceType, typeInfo.getHighCpuCore());
            } else if (targetInstanceType == InstanceType.LOW) {
                actualInstanceType = typeInfo.getLowInstanceType();
                logger.info("選択された LOW インスタンスタイプ: {} (CPU コア数: {})",
                        actualInstanceType, typeInfo.getLowCpuCore());
            } else if (targetInstanceType == InstanceType.VERYLOW) {
                actualInstanceType = typeInfo.getVeryLowInstanceType();
                logger.info("選択された VERYLOW インスタンスタイプ: {} (CPU コア数: {})",
                        actualInstanceType, typeInfo.getVeryLowCpuCore());
            } else {
                logger.error("無効な targetInstanceType: {}", targetInstanceType);
                return;
            }

            // 5. 外部シェルを呼び出してインスタンスタイプを変更（空実装）
            executeInstanceTypeChange(hostname, actualInstanceType);

            // 6. インスタンスタイプ変更確認スレッドを起動
            startMonitoringThread(hostname, targetInstanceType);

        } catch (IOException e) {
            logger.error("ホスト名 {} のインスタンスタイプ変更に失敗しました", hostname, e);
            throw new RuntimeException("Failed to change instance type", e);
        }
    }

    /**
     * 外部シェルを呼び出してインスタンスタイプを変更します
     * 
     * @param hostname     ホスト名
     * @param instanceType インスタンスタイプ
     */
    private void executeInstanceTypeChange(String hostname, String instanceType) {
        logger.info("ホスト名 {} のインスタンスタイプ変更を実行します: {}",
                hostname, instanceType);

        try {
            // 外部シェルを実行 (引数: ホスト名, インスタンスタイプ)
            String output = shellExecutor.executeShell(
                    executeInstanceTypeChangeShellPath,
                    List.of(hostname, instanceType),
                    shellTimeoutSeconds);

            logger.info("ホスト名 {} のインスタンスタイプ変更が開始されました: {}", hostname, output.trim());

        } catch (Exception e) {
            logger.error("ホスト名 {} のインスタンスタイプ変更に失敗しました", hostname, e);
            throw new RuntimeException("インスタンスタイプ変更に失敗しました: " + hostname, e);
        }
    }

    /**
     * インスタンスタイプ変更確認スレッドを起動します
     * 定期的にインスタンスタイプ変更完了を確認し、完了したらInstanceStatus.csvを更新します
     * 
     * @param hostname           ホスト名
     * @param targetInstanceType 変更先のインスタンスタイプ
     */
    private void startMonitoringThread(String hostname, InstanceType targetInstanceType) {
        logger.info("ホスト名 {} の監視スレッドを開始します", hostname);

        AtomicInteger retryCount = new AtomicInteger(0);
        scheduleNextCheck(hostname, targetInstanceType, retryCount, 1);
    }

    private void scheduleNextCheck(String hostname, InstanceType targetInstanceType, AtomicInteger retryCount,
            long delaySeconds) {
        monitoringExecutor.schedule(() -> {
            try {
                int currentRetry = retryCount.incrementAndGet();
                logger.debug("ホスト名 {} のインスタンスタイプ変更完了をチェック中 (試行 {}/{})",
                        hostname, currentRetry, maxRetryCount);

                boolean isCompleted = checkInstanceTypeChangeCompletion(hostname);

                if (isCompleted) {
                    logger.info("ホスト名 {} のインスタンスタイプ変更が完了しました。InstanceStatus.csv を更新します",
                            hostname);

                    // InstanceStatus.csvのInstanceTypeカラムを更新
                    instanceStatusRepository.updateInstanceType(hostname, targetInstanceType);

                    logger.info("ホスト名 {} のインスタンスタイプを {} に更新しました",
                            hostname, targetInstanceType);
                    return; // 完了
                }

                if (currentRetry >= maxRetryCount) {
                    logger.warn("ホスト名 {} の最大リトライ回数に達しました。監視スレッドを停止します",
                            hostname);
                    return; // 最大リトライ回数到達
                }

                // 次回のチェックをスケジュール
                scheduleNextCheck(hostname, targetInstanceType, retryCount, checkIntervalSeconds);

            } catch (Exception e) {
                logger.error("ホスト名 {} の監視スレッドでエラーが発生しました", hostname, e);
                if (retryCount.get() < maxRetryCount) {
                    scheduleNextCheck(hostname, targetInstanceType, retryCount, checkIntervalSeconds);
                }
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * インスタンスタイプ変更完了を確認します
     * 外部シェルを呼び出して確認します
     * 
     * @param hostname ホスト名
     * @return 変更完了の場合true
     */
    protected boolean checkInstanceTypeChangeCompletion(String hostname) {
        logger.debug("ホスト名 {} のインスタンスタイプ変更完了をチェック中", hostname);

        try {
            // 外部シェルを実行 (引数: ホスト名)
            String output = shellExecutor.executeShell(
                    checkInstanceTypeChangeShellPath,
                    List.of(hostname),
                    shellTimeoutSeconds);

            // 標準出力が "COMPLETED" なら完了、"IN_PROGRESS" なら進行中
            String status = output.trim();
            if ("COMPLETED".equals(status)) {
                logger.info("ホスト名 {} のインスタンスタイプ変更が完了しています", hostname);
                return true;
            } else {
                logger.debug("ホスト名 {} のインスタンスタイプ変更はまだ進行中です: {}", hostname, status);
                return false;
            }

        } catch (Exception e) {
            logger.warn("ホスト名 {} のインスタンスタイプ変更確認に失敗しました。未完了とみなします", hostname, e);
            return false;
        }
    }
}
