package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.*;
import com.example.jsonreceiver.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    private final ScheduledExecutorService monitoringExecutor = Executors.newScheduledThreadPool(5);

    @Value("${instance-type-change.check-interval-seconds:5}")
    private int checkIntervalSeconds;

    @Value("${instance-type-change.max-retry-count:10}")
    private int maxRetryCount;

    /**
     * インスタンスタイプを変更します
     * SystemInfo.csv → InstanceTypeLink.csv → InstanceType.csvからインスタンスタイプを取得し、
     * 外部シェルを呼び出してインスタンスタイプを変更します（外部呼び出しは空実装）
     * 
     * @param hostname           ホスト名
     * @param targetInstanceType 変更先のインスタンスタイプ
     */
    public void changeInstanceType(String hostname, InstanceType targetInstanceType) {
        logger.info("Changing instance type for hostname: {} to: {}", hostname, targetInstanceType);

        try {
            // 1. SystemInfo.csvからホスト名でElTypeを取得
            Optional<SystemInfo> systemInfoOpt = systemInfoRepository.findByHostname(hostname);
            if (systemInfoOpt.isEmpty()) {
                logger.error("SystemInfo not found for hostname: {}", hostname);
                return;
            }
            SystemInfo systemInfo = systemInfoOpt.get();
            String elType = systemInfo.getElType();
            logger.debug("Found ElType: {} for hostname: {}", elType, hostname);

            // 2. InstanceTypeLink.csvからElTypeでInstanceTypeIdを取得
            Optional<InstanceTypeLink> linkOpt = instanceTypeLinkRepository.findByElType(elType);
            if (linkOpt.isEmpty()) {
                logger.error("InstanceTypeLink not found for ElType: {}", elType);
                return;
            }
            InstanceTypeLink link = linkOpt.get();
            String instanceTypeId = link.getInstanceTypeId();
            logger.debug("Found InstanceTypeId: {} for ElType: {}", instanceTypeId, elType);

            // 3. InstanceType.csvからInstanceTypeIdで対応するインスタンスタイプを取得
            Optional<InstanceTypeInfo> typeInfoOpt = instanceTypeRepository.findByInstanceTypeId(instanceTypeId);
            if (typeInfoOpt.isEmpty()) {
                logger.error("InstanceType not found for InstanceTypeId: {}", instanceTypeId);
                return;
            }
            InstanceTypeInfo typeInfo = typeInfoOpt.get();

            // 4. targetInstanceTypeに応じて適切なインスタンスタイプを選択
            String actualInstanceType;
            if (targetInstanceType == InstanceType.HIGH) {
                actualInstanceType = typeInfo.getHighInstanceType();
                logger.info("Selected HIGH instance type: {} (CPU cores: {})",
                        actualInstanceType, typeInfo.getHighCpuCore());
            } else if (targetInstanceType == InstanceType.LOW) {
                actualInstanceType = typeInfo.getLowInstanceType();
                logger.info("Selected LOW instance type: {} (CPU cores: {})",
                        actualInstanceType, typeInfo.getLowCpuCore());
            } else if (targetInstanceType == InstanceType.VERYLOW) {
                actualInstanceType = typeInfo.getVeryLowInstanceType();
                logger.info("Selected VERYLOW instance type: {} (CPU cores: {})",
                        actualInstanceType, typeInfo.getVeryLowCpuCore());
            } else {
                logger.error("Invalid targetInstanceType: {}", targetInstanceType);
                return;
            }

            // 5. 外部シェルを呼び出してインスタンスタイプを変更（空実装）
            executeInstanceTypeChange(hostname, actualInstanceType);

            // 6. インスタンスタイプ変更確認スレッドを起動
            startMonitoringThread(hostname, targetInstanceType);

        } catch (IOException e) {
            logger.error("Failed to change instance type for hostname: {}", hostname, e);
            throw new RuntimeException("Failed to change instance type", e);
        }
    }

    /**
     * 外部シェルを呼び出してインスタンスタイプを変更します（空実装）
     * 
     * @param hostname     ホスト名
     * @param instanceType インスタンスタイプ
     */
    private void executeInstanceTypeChange(String hostname, String instanceType) {
        logger.info("Executing instance type change for hostname: {} to: {} (stub implementation)",
                hostname, instanceType);
        // TODO: 外部シェルを呼び出してインスタンスタイプを変更
    }

    /**
     * インスタンスタイプ変更確認スレッドを起動します
     * 定期的にインスタンスタイプ変更完了を確認し、完了したらInstanceStatus.csvを更新します
     * 
     * @param hostname           ホスト名
     * @param targetInstanceType 変更先のインスタンスタイプ
     */
    private void startMonitoringThread(String hostname, InstanceType targetInstanceType) {
        logger.info("Starting monitoring thread for hostname: {}", hostname);

        AtomicInteger retryCount = new AtomicInteger(0);
        scheduleNextCheck(hostname, targetInstanceType, retryCount, 1);
    }

    private void scheduleNextCheck(String hostname, InstanceType targetInstanceType, AtomicInteger retryCount,
            long delaySeconds) {
        monitoringExecutor.schedule(() -> {
            try {
                int currentRetry = retryCount.incrementAndGet();
                logger.debug("Checking instance type change completion for hostname: {} (attempt {}/{})",
                        hostname, currentRetry, maxRetryCount);

                boolean isCompleted = checkInstanceTypeChangeCompletion(hostname);

                if (isCompleted) {
                    logger.info("Instance type change completed for hostname: {}, updating InstanceStatus.csv",
                            hostname);

                    // InstanceStatus.csvのInstanceTypeカラムを更新
                    instanceStatusRepository.updateInstanceType(hostname, targetInstanceType);

                    logger.info("Successfully updated InstanceType to {} for hostname: {}",
                            targetInstanceType, hostname);
                    return; // 完了
                }

                if (currentRetry >= maxRetryCount) {
                    logger.warn("Max retry count reached for hostname: {}, stopping monitoring thread",
                            hostname);
                    return; // 最大リトライ回数到達
                }

                // 次回のチェックをスケジュール
                scheduleNextCheck(hostname, targetInstanceType, retryCount, checkIntervalSeconds);

            } catch (Exception e) {
                logger.error("Error in monitoring thread for hostname: {}", hostname, e);
                // エラーが発生してもリトライ上限までは継続するか、ここで停止するか。
                // ここでは安全のため停止せず、次回のスケジュールを行う（リトライカウントは増えている）
                if (retryCount.get() < maxRetryCount) {
                    scheduleNextCheck(hostname, targetInstanceType, retryCount, checkIntervalSeconds);
                }
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * インスタンスタイプ変更完了を確認します（空実装）
     * 
     * @param hostname ホスト名
     * @return 変更完了の場合true
     */
    protected boolean checkInstanceTypeChangeCompletion(String hostname) {
        logger.debug("Checking instance type change completion for hostname: {} (stub implementation)", hostname);
        // TODO: 外部シェルを呼び出してインスタンスタイプ変更完了を確認
        // 現在は空実装のため、即座に完了とみなす
        return true;
    }
}
