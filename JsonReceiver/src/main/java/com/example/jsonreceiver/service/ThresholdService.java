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
                logger.error("ホスト名 {} のしきい値が見つかりません", hostname);
                return;
            }

            ThresholdInfo threshold = thresholdOpt.get();
            logger.info("ホスト名 {} のしきい値をチェック中 CPU={} Memory={}", hostname, cpuUsage, memoryUsage);

            // しきい値を超えているか判定
            InstanceType targetInstanceType = null;
            boolean cpuExceedsUpper = cpuUsage > threshold.getCpuUpperLimit();
            boolean memoryExceedsUpper = memoryUsage > threshold.getMemoryUpperLimit();
            boolean cpuBelowLower = cpuUsage < threshold.getCpuLowerLimit();
            boolean memoryBelowLower = memoryUsage < threshold.getMemoryLowerLimit();

            if (cpuExceedsUpper || memoryExceedsUpper) {
                targetInstanceType = InstanceType.HIGH;
                logger.info("ホスト名 {} のメトリクスが上限を超えています (CPU: {}, Memory: {})",
                        hostname, cpuUsage, memoryUsage);
            } else if (cpuBelowLower || memoryBelowLower) {
                targetInstanceType = InstanceType.LOW;
                logger.info("ホスト名 {} のメトリクスが下限を下回っています (CPU: {}, Memory: {})",
                        hostname, cpuUsage, memoryUsage);
            }

            if (targetInstanceType == null) {
                logger.info("ホスト名 {} のメトリクスはしきい値内です", hostname);
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
                logger.warn("ホスト名 {} の履歴データが不十分です (必要: {}件、取得: {}件)",
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
                logger.info("ホスト名 {} の連続しきい値超過を検出しました (回数: {})", hostname,
                        continueCount);
                // Check current instance type to avoid unnecessary change
                try {
                    InstanceStatus currentStatus = instanceStatusRepository
                            .findByHostname(hostname).orElse(null);
                    if (currentStatus != null && currentStatus.getInstanceType() == targetInstanceType) {
                        logger.warn("インスタンスタイプは既に {} です。ホスト名: {}。変更は実行されません。",
                                targetInstanceType, hostname);
                        return;
                    }
                } catch (java.io.IOException e) {
                    logger.error("ホスト名 {} の現在のインスタンスステータスの取得に失敗しました", hostname, e);
                    // Proceed with change despite the error
                }
                instanceTypeChangeService.changeInstanceType(hostname, targetInstanceType);
            } else {
                logger.debug("ホスト名 {} のしきい値超過は連続していません", hostname);
            }

        } catch (IOException e) {
            logger.error("しきい値チェックに失敗しました", e);
            throw new RuntimeException("Failed to check threshold", e);
        }
    }
}
