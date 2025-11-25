package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.InstanceType;
import com.example.jsonreceiver.dto.MetricsJson;
import com.example.jsonreceiver.dto.ResourceInfo;
import com.example.jsonreceiver.dto.ThresholdInfo;
import com.example.jsonreceiver.repository.ResourceInfoRepository;
import com.example.jsonreceiver.repository.ThresholdRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.example.jsonreceiver.dto.InstanceStatus;
import com.example.jsonreceiver.repository.InstanceStatusRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * しきい値判定サービス
 * メトリクスがしきい値を超えているかチェックし、連続回数に基づいてインスタンスタイプ変更を実行します
 */
@Service
@RequiredArgsConstructor
public class ThresholdService {

    private static final Logger logger = LoggerFactory.getLogger(ThresholdService.class);

    private final ResourceInfoRepository resourceInfoRepository;
    private final ThresholdRepository thresholdRepository;
    private final InstanceTypeChangeService instanceTypeChangeService;
    // Added repository to fetch current instance type
    private final InstanceStatusRepository instanceStatusRepository;

    /**
     * しきい値チェックを実行します
     * 
     * @param metricsJson メトリクスJSON
     */
    public void checkThreshold(MetricsJson metricsJson) {
        try {
            String hostname = metricsJson.getInstanceName();
            double cpuUsage = metricsJson.getMetrics().getCpuUsage();
            double memoryUsage = metricsJson.getMetrics().getMemoryUsage();

            // しきい値を取得
            Optional<ThresholdInfo> thresholdOpt = thresholdRepository.findByHostname(hostname);
            if (thresholdOpt.isEmpty()) {
                logger.error("No threshold found for hostname: {}", hostname);
                return;
            }

            ThresholdInfo threshold = thresholdOpt.get();
            logger.info("Checking threshold for hostname: {} CPU={} Memory={}", hostname, cpuUsage, memoryUsage);

            // しきい値を超えているか判定
            InstanceType targetInstanceType = null;
            boolean cpuExceedsUpper = cpuUsage > threshold.getCpuUpperLimit();
            boolean memoryExceedsUpper = memoryUsage > threshold.getMemoryUpperLimit();
            boolean cpuBelowLower = cpuUsage < threshold.getCpuLowerLimit();
            boolean memoryBelowLower = memoryUsage < threshold.getMemoryLowerLimit();

            if (cpuExceedsUpper || memoryExceedsUpper) {
                targetInstanceType = InstanceType.HIGH;
                logger.info("Metrics exceed upper limit for hostname: {} (CPU: {}, Memory: {})",
                        hostname, cpuUsage, memoryUsage);
            } else if (cpuBelowLower || memoryBelowLower) {
                targetInstanceType = InstanceType.LOW;
                logger.info("Metrics below lower limit for hostname: {} (CPU: {}, Memory: {})",
                        hostname, cpuUsage, memoryUsage);
            }

            if (targetInstanceType == null) {
                logger.error("Metrics within threshold for hostname: {}", hostname);
                return;
            }

            // 過去のデータを取得して連続回数を確認
            int continueCount = threshold.getContinueCount();
            if (continueCount <= 1) {
                // 継続回数が1以下の場合は即座に変更
                instanceTypeChangeService.changeInstanceType(hostname, targetInstanceType);
                return;
            }

            // 過去 (continueCount - 1) 件のデータを取得
            List<ResourceInfo> history = resourceInfoRepository.findLastNByHostname(hostname, continueCount - 1);

            // 連続でしきい値を超えているかチェック
            if (history.size() < continueCount - 1) {
                logger.warn("Insufficient history for hostname: {} (need {}, got {})",
                        hostname, continueCount - 1, history.size());
                return;
            }

            // 全ての過去データが同じしきい値超過状態かチェック
            boolean allExceed = true;
            for (ResourceInfo info : history) {
                boolean histCpuExceedsUpper = info.getCpuUsage() > threshold.getCpuUpperLimit();
                boolean histMemoryExceedsUpper = info.getMemoryUsage() > threshold.getMemoryUpperLimit();
                boolean histCpuBelowLower = info.getCpuUsage() < threshold.getCpuLowerLimit();
                boolean histMemoryBelowLower = info.getMemoryUsage() < threshold.getMemoryLowerLimit();

                boolean historyExceedsUpper = histCpuExceedsUpper || histMemoryExceedsUpper;
                boolean historyBelowLower = histCpuBelowLower || histMemoryBelowLower;

                if (targetInstanceType == InstanceType.HIGH && !historyExceedsUpper) {
                    allExceed = false;
                    break;
                } else if (targetInstanceType == InstanceType.LOW && !historyBelowLower) {
                    allExceed = false;
                    break;
                }
            }

            if (allExceed) {
                logger.info("Continuous threshold violation detected for hostname: {} (count: {})", hostname,
                        continueCount);
                // Check current instance type to avoid unnecessary change
                try {
                    InstanceStatus currentStatus = instanceStatusRepository
                            .findByHostname(hostname).orElse(null);
                    if (currentStatus != null && currentStatus.getInstanceType() == targetInstanceType) {
                        logger.warn("Instance type is already {} for hostname: {}. No change performed.",
                                targetInstanceType, hostname);
                        return;
                    }
                } catch (java.io.IOException e) {
                    logger.error("Failed to retrieve current instance status for hostname: {}", hostname, e);
                    // Proceed with change despite the error
                }
                instanceTypeChangeService.changeInstanceType(hostname, targetInstanceType);
            } else {
                logger.debug("Threshold violation not continuous for hostname: {}", hostname);
            }

        } catch (IOException e) {
            logger.error("Failed to check threshold", e);
            throw new RuntimeException("Failed to check threshold", e);
        }
    }
}
