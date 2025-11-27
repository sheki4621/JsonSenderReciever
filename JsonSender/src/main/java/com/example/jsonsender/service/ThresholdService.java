package com.example.jsonsender.service;

import com.example.jsoncommon.dto.ConditionLogic;
import com.example.jsoncommon.dto.InstanceTypeChangeRequest;
import com.example.jsoncommon.dto.ThresholdInfo;
import com.example.jsoncommon.repository.ResourceHistoryRepository;
import com.example.jsoncommon.dto.ResourceInfo;
import com.example.jsonsender.repository.ThresholdRepository;
import com.example.jsonsender.utils.HostnameUtil;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZonedDateTime;
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

    private final ThresholdRepository thresholdRepository;
    private final ResourceHistoryRepository resourceHistoryRepository;

    /**
     * しきい値チェックを実行します
     * 
     * @param cpuUsage    CPU使用率
     * @param memoryUsage メモリ使用率
     */
    public InstanceTypeChangeRequest checkThreshold(double cpuUsage, double memoryUsage) {
        try {
            String hostname = HostnameUtil.getHostname();

            logger.info("ホスト名 {} のしきい値をチェック中 CPU={} Memory={}", hostname, cpuUsage, memoryUsage);

            // しきい値を取得
            Optional<ThresholdInfo> thresholdOpt = thresholdRepository.findByHostname(hostname);
            if (thresholdOpt.isEmpty()) {
                logger.error("ホスト名 {} のしきい値ファイルが見つかりません", hostname);
                throw new IOException(String.format("ホスト名 %s のしきい値ファイルが見つかりません", hostname));
            }

            ThresholdInfo threshold = thresholdOpt.get();
            logger.debug("ホスト名 {} のしきい値を取得しました {}", hostname, threshold);

            // しきい値を超えているか判定
            boolean cpuExceedsUpper = cpuUsage > threshold.getUpperCpuThreshold();
            boolean memoryExceedsUpper = memoryUsage > threshold.getUpperMemThreshold();
            boolean cpuBelowLower = cpuUsage < threshold.getLowerCpuThreshold();
            boolean memoryBelowLower = memoryUsage < threshold.getLowerMemThreshold();

            // CPU UPPER判定
            boolean isCpuUpperRequest = false;
            if (cpuExceedsUpper) {
                logger.info("CPU使用率がしきい値を上回りました CPU使用率: {}, しきい値(UPPER_CPU_THRESHOLD): {}", cpuUsage,
                        threshold.getUpperCpuThreshold());

                List<ResourceInfo> recentHistory = resourceHistoryRepository.findRecentByHostname(hostname,
                        threshold.getUpperCpuDurationMin());
                ZonedDateTime beginTime = getBeginTimeCpuExceedsUpper(recentHistory, threshold.getUpperCpuThreshold());
                logger.info("{}からCPU使用率がしきい値を上回った状態が継続しています。", beginTime);
                if (beginTime != null
                        && beginTime.isAfter(ZonedDateTime.now().minusMinutes(threshold.getUpperCpuDurationMin()))) {
                    isCpuUpperRequest = true;
                }
            }

            // MEMORY UPPER判定
            boolean isMemoryUpperRequest = false;
            if (memoryExceedsUpper) {
                logger.info("メモリ使用率がしきい値を上回りました メモリ使用率: {}, しきい値(UPPER_MEM_THRESHOLD): {}", memoryUsage,
                        threshold.getUpperMemThreshold());

                List<ResourceInfo> recentHistory = resourceHistoryRepository.findRecentByHostname(hostname,
                        threshold.getUpperMemDurationMin());
                ZonedDateTime beginTime = getBeginTimeMemoryExceedsUpper(recentHistory,
                        threshold.getUpperMemThreshold());
                logger.info("{}からメモリ使用率がしきい値を上回った状態が継続しています。", beginTime);
                if (beginTime != null
                        && beginTime.isAfter(ZonedDateTime.now().minusMinutes(threshold.getUpperMemDurationMin()))) {
                    isMemoryUpperRequest = true;
                }
            }

            // CPU LOWER判定
            boolean isCpuLowerRequest = false;
            if (cpuBelowLower) {
                logger.info("CPU使用率がしきい値を下回りました CPU使用率: {}, しきい値(LOWER_CPU_THRESHOLD): {}", cpuUsage,
                        threshold.getLowerCpuThreshold());

                List<ResourceInfo> recentHistory = resourceHistoryRepository.findRecentByHostname(hostname,
                        threshold.getLowerCpuDurationMin());
                ZonedDateTime beginTime = getBeginTimeCpuBelowLower(recentHistory, threshold.getLowerCpuThreshold());
                logger.info("{}からCPU使用率がしきい値を下回った状態が継続しています。", beginTime);
                if (beginTime != null
                        && beginTime.isAfter(ZonedDateTime.now().minusMinutes(threshold.getLowerCpuDurationMin()))) {
                    isCpuLowerRequest = true;
                }
            }

            // MEMORY LOWER判定
            boolean isMemoryLowerRequest = false;
            if (memoryBelowLower) {
                logger.info("メモリ使用率がしきい値を下回りました メモリ使用率: {}, しきい値(LOWER_MEM_THRESHOLD): {}", memoryUsage,
                        threshold.getLowerMemThreshold());

                List<ResourceInfo> recentHistory = resourceHistoryRepository.findRecentByHostname(hostname,
                        threshold.getLowerMemDurationMin());
                ZonedDateTime beginTime = getBeginTimeMemoryBelowLower(recentHistory, threshold.getLowerMemThreshold());
                logger.info("{}からメモリ使用率がしきい値を下回った状態が継続しています。", beginTime);
                if (beginTime != null
                        && beginTime.isAfter(ZonedDateTime.now().minusMinutes(threshold.getLowerMemDurationMin()))) {
                    isMemoryLowerRequest = true;
                }
            }

            // UPPER判定
            if (threshold.getUpperConditionLogic() == ConditionLogic.OR) {
                if (isCpuUpperRequest || isMemoryUpperRequest) {
                    logger.info("ホスト名 {} のメトリクスがCPUまたはMemoryのしきい値を超えています (CPU: {}, Memory: {})",
                            hostname, cpuUsage, memoryUsage);
                    return InstanceTypeChangeRequest.UPPER;
                }

            } else if (threshold.getUpperConditionLogic() == ConditionLogic.AND) {
                if (isCpuUpperRequest && isMemoryUpperRequest) {
                    logger.info("ホスト名 {} のメトリクスがCPUとMemoryともにしきい値を超えています (CPU: {}, Memory: {})",
                            hostname, cpuUsage, memoryUsage);
                    return InstanceTypeChangeRequest.UPPER;
                }
            }

            // LOWER判定
            if (threshold.getLowerConditionLogic() == ConditionLogic.OR) {
                if (isCpuLowerRequest || isMemoryLowerRequest) {
                    logger.info("ホスト名 {} のメトリクスがCPUまたはMemoryのしきい値を下回っています (CPU: {}, Memory: {})",
                            hostname, cpuUsage, memoryUsage);
                    return InstanceTypeChangeRequest.LOWER;
                }

            } else if (threshold.getLowerConditionLogic() == ConditionLogic.AND) {
                if (isCpuLowerRequest && isMemoryLowerRequest) {
                    logger.info("ホスト名 {} のメトリクスがCPUとMemoryともにしきい値を下回っています (CPU: {}, Memory: {})",
                            hostname, cpuUsage, memoryUsage);
                    return InstanceTypeChangeRequest.LOWER;
                }
            }

            logger.info("ホスト名 {} のメトリクスはしきい値内です", hostname);
            return InstanceTypeChangeRequest.WITHIN;

        } catch (IOException e) {
            logger.error("しきい値チェックに失敗しました", e);
            throw new RuntimeException("Failed to check threshold", e);
        }
    }

    /**
     * CPU使用率がしきい値を上回っている状態が始まった時刻を取得
     * 
     * @param history           リソース情報のリスト
     * @param upperCpuThreshold 上限しきい値
     * @return CPU使用率がしきい値を上回っている状態が始まった時刻(null: しきい値を上回っていない)
     */
    private ZonedDateTime getBeginTimeCpuExceedsUpper(List<ResourceInfo> history, Double upperCpuThreshold) {
        if (history == null || history.isEmpty()) {
            return null;
        }

        ZonedDateTime beginTime = null;
        for (int i = 0; i < history.size(); i++) {
            ResourceInfo resourceInfo = history.get(i);
            if (resourceInfo.getCpuUsage() != null && resourceInfo.getCpuUsage() > upperCpuThreshold) {
                beginTime = ZonedDateTime.parse(resourceInfo.getTimestamp());
            } else {
                return beginTime;
            }
        }
        return beginTime;
    }

    /**
     * メモリ使用率がしきい値を上回っている状態が始まった時刻を取得
     * 
     * @param history              リソース情報のリスト
     * @param upperMemoryThreshold 上限しきい値
     * @return メモリ使用率がしきい値を上回っている状態が始まった時刻(null: しきい値を上回っていない)
     */
    private ZonedDateTime getBeginTimeMemoryExceedsUpper(List<ResourceInfo> history, Double upperMemoryThreshold) {
        if (history == null || history.isEmpty()) {
            return null;
        }

        ZonedDateTime beginTime = null;
        for (int i = 0; i < history.size(); i++) {
            ResourceInfo resourceInfo = history.get(i);
            if (resourceInfo.getMemoryUsage() != null && resourceInfo.getMemoryUsage() > upperMemoryThreshold) {
                beginTime = ZonedDateTime.parse(resourceInfo.getTimestamp());
            } else {
                return beginTime;
            }
        }
        return beginTime;
    }

    /**
     * CPU使用率がしきい値を下回っている状態が始まった時刻を取得
     * 
     * @param history           リソース情報のリスト
     * @param lowerCpuThreshold 下限しきい値
     * @return CPU使用率がしきい値を下回っている状態が始まった時刻(null: しきい値を下回っていない)
     */
    private ZonedDateTime getBeginTimeCpuBelowLower(List<ResourceInfo> history, Double lowerCpuThreshold) {
        if (history == null || history.isEmpty()) {
            return null;
        }

        ZonedDateTime beginTime = null;
        for (int i = 0; i < history.size(); i++) {
            ResourceInfo resourceInfo = history.get(i);
            if (resourceInfo.getCpuUsage() != null && resourceInfo.getCpuUsage() < lowerCpuThreshold) {
                beginTime = ZonedDateTime.parse(resourceInfo.getTimestamp());
            } else {
                return beginTime;
            }
        }
        return beginTime;
    }

    /**
     * メモリ使用率がしきい値を下回っている状態が始まった時刻を取得
     * 
     * @param history              リソース情報のリスト
     * @param lowerMemoryThreshold 下限しきい値
     * @return メモリ使用率がしきい値を下回っている状態が始まった時刻(null: しきい値を下回っていない)
     */
    private ZonedDateTime getBeginTimeMemoryBelowLower(List<ResourceInfo> history, Double lowerMemoryThreshold) {
        if (history == null || history.isEmpty()) {
            return null;
        }

        ZonedDateTime beginTime = null;
        for (int i = 0; i < history.size(); i++) {
            ResourceInfo resourceInfo = history.get(i);
            if (resourceInfo.getMemoryUsage() != null && resourceInfo.getMemoryUsage() < lowerMemoryThreshold) {
                beginTime = ZonedDateTime.parse(resourceInfo.getTimestamp());
            } else {
                return beginTime;
            }
        }
        return beginTime;
    }
}