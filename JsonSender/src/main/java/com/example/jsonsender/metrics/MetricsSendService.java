package com.example.jsonsender.metrics;

import com.example.jsoncommon.dto.ConditionLogic;
import com.example.jsoncommon.dto.InstanceTypeChangeRequest;
import com.example.jsoncommon.dto.Metrics;
import com.example.jsoncommon.dto.ResourceHistoryCsv;
import com.example.jsoncommon.dto.ThresholdCsv;
import com.example.jsoncommon.repository.ResourceHistoryRepository;
import com.example.jsonsender.repository.ThresholdRepository;
import com.example.jsoncommon.util.HostnameUtil;
import com.example.jsoncommon.util.CommandExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsSendService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsSendService.class);

    private final ThresholdRepository thresholdRepository;
    private final ResourceHistoryRepository resourceHistoryRepository;
    private final CommandExecutor shellExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${metrics.shell.path}")
    private String metricsShellPath;

    @Value("${metrics.shell.timeout}")
    private int shellTimeoutSeconds;

    public Metrics collect() {
        Metrics metrics = getCpuMemoryUsage();

        // CPU/メモリ使用率がnullでない場合のみしきい値チェック
        if (metrics.getCpuUsage() != null && metrics.getMemoryUsage() != null) {
            try {
                InstanceTypeChangeRequest instanceTypeChangeRequest = getInstanceTypeChangeRequest(
                        metrics.getCpuUsage(), metrics.getMemoryUsage());
                return new Metrics(metrics.getCpuUsage(), metrics.getMemoryUsage(), instanceTypeChangeRequest);
            } catch (Exception e) {
                logger.error("しきい値チェック中にエラーが発生しました: " + e.getMessage());
            }
        }

        return metrics;
    }

    /**
     * CPU使用率とメモリ使用率を外部シェルから取得します
     * 
     * @return メトリクス情報
     */
    protected Metrics getCpuMemoryUsage() {
        try {
            logger.debug("メトリクス収集シェルを実行します: {}", metricsShellPath);

            // String output = shellExecutor.executeCommand(metricsShellPath,
            // Collections.emptyList(), shellTimeoutSeconds);

            // 提供されるまで仮の値で処理
            String output = "{\"CpuUsage\": 10.5,\"MemoryUsage\": 20.3}";

            // JSONをパース
            JsonNode jsonNode = objectMapper.readTree(output);

            Double cpuUsage = null;
            Double memoryUsage = null;

            if (jsonNode.has("CpuUsage") && !jsonNode.get("CpuUsage").isNull()) {
                cpuUsage = jsonNode.get("CpuUsage").asDouble();
            }

            if (jsonNode.has("MemoryUsage") && !jsonNode.get("MemoryUsage").isNull()) {
                memoryUsage = jsonNode.get("MemoryUsage").asDouble();
            }

            logger.info("メトリクスを取得しました CPU使用率: {}, メモリ使用率: {}", cpuUsage, memoryUsage);

            return new Metrics(cpuUsage, memoryUsage, null);

        } catch (IOException e) {
            logger.error("メトリクス収集シェルの実行に失敗しました: {}", e.getMessage());
            return new Metrics(null, null, null);
        } catch (Exception e) {
            logger.error("メトリクスのJSONパースに失敗しました: {}", e.getMessage());
            return new Metrics(null, null, null);
        }
    }

    /**
     * InstanceTypeChangeRequestを取得します
     * 
     * @param cpuUsage    CPU使用率
     * @param memoryUsage メモリ使用率
     */
    public InstanceTypeChangeRequest getInstanceTypeChangeRequest(double cpuUsage, double memoryUsage) {
        try {
            String hostname = HostnameUtil.getHostname();

            logger.info("ホスト名 {} のしきい値をチェック中 CPU={} Memory={}", hostname, cpuUsage, memoryUsage);

            // しきい値を取得
            Optional<ThresholdCsv> thresholdOpt = thresholdRepository.findByHostname(hostname);
            if (thresholdOpt.isEmpty()) {
                logger.error("しきい値ファイルが見つかりません: {}", thresholdRepository.getFilePath());
                return null;
            }

            ThresholdCsv threshold = thresholdOpt.get();
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

                List<ResourceHistoryCsv> recentHistory = resourceHistoryRepository.findRecentByHostname(hostname,
                        threshold.getUpperCpuDurationMin());
                ZonedDateTime beginTime = getBeginTimeCpuExceedsUpper(recentHistory, threshold.getUpperCpuThreshold());
                logger.info("{}からCPU使用率がしきい値を上回った状態が継続しています。", beginTime);
                // しきい値を超えた状態が指定分数以上継続している場合
                if (beginTime != null
                        && beginTime.isBefore(ZonedDateTime.now().minusMinutes(threshold.getUpperCpuDurationMin()))) {
                    isCpuUpperRequest = true;
                }
            }

            // MEMORY UPPER判定
            boolean isMemoryUpperRequest = false;
            if (memoryExceedsUpper) {
                logger.info("メモリ使用率がしきい値を上回りました メモリ使用率: {}, しきい値(UPPER_MEM_THRESHOLD): {}", memoryUsage,
                        threshold.getUpperMemThreshold());

                List<ResourceHistoryCsv> recentHistory = resourceHistoryRepository.findRecentByHostname(hostname,
                        threshold.getUpperMemDurationMin());
                ZonedDateTime beginTime = getBeginTimeMemoryExceedsUpper(recentHistory,
                        threshold.getUpperMemThreshold());
                logger.info("{}からメモリ使用率がしきい値を上回った状態が継続しています。", beginTime);
                // しきい値を超えた状態が指定分数以上継続している場合
                if (beginTime != null
                        && beginTime.isBefore(ZonedDateTime.now().minusMinutes(threshold.getUpperMemDurationMin()))) {
                    isMemoryUpperRequest = true;
                }
            }

            // CPU LOWER判定
            boolean isCpuLowerRequest = false;
            if (cpuBelowLower) {
                logger.info("CPU使用率がしきい値を下回りました CPU使用率: {}, しきい値(LOWER_CPU_THRESHOLD): {}", cpuUsage,
                        threshold.getLowerCpuThreshold());

                List<ResourceHistoryCsv> recentHistory = resourceHistoryRepository.findRecentByHostname(hostname,
                        threshold.getLowerCpuDurationMin());
                ZonedDateTime beginTime = getBeginTimeCpuBelowLower(recentHistory, threshold.getLowerCpuThreshold());
                logger.info("{}からCPU使用率がしきい値を下回った状態が継続しています。", beginTime);
                // しきい値を下回った状態が指定分数以上継続している場合
                if (beginTime != null
                        && beginTime.isBefore(ZonedDateTime.now().minusMinutes(threshold.getLowerCpuDurationMin()))) {
                    isCpuLowerRequest = true;
                }
            }

            // MEMORY LOWER判定
            boolean isMemoryLowerRequest = false;
            if (memoryBelowLower) {
                logger.info("メモリ使用率がしきい値を下回りました メモリ使用率: {}, しきい値(LOWER_MEM_THRESHOLD): {}", memoryUsage,
                        threshold.getLowerMemThreshold());

                List<ResourceHistoryCsv> recentHistory = resourceHistoryRepository.findRecentByHostname(hostname,
                        threshold.getLowerMemDurationMin());
                ZonedDateTime beginTime = getBeginTimeMemoryBelowLower(recentHistory, threshold.getLowerMemThreshold());
                logger.info("{}からメモリ使用率がしきい値を下回った状態が継続しています。", beginTime);
                // しきい値を下回った状態が指定分数以上継続している場合
                if (beginTime != null
                        && beginTime.isBefore(ZonedDateTime.now().minusMinutes(threshold.getLowerMemDurationMin()))) {
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
    private ZonedDateTime getBeginTimeCpuExceedsUpper(List<ResourceHistoryCsv> history, Double upperCpuThreshold) {
        if (history == null || history.isEmpty()) {
            return null;
        }

        ZonedDateTime beginTime = null;
        for (int i = 0; i < history.size(); i++) {
            ResourceHistoryCsv resourceInfo = history.get(i);
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
    private ZonedDateTime getBeginTimeMemoryExceedsUpper(List<ResourceHistoryCsv> history,
            Double upperMemoryThreshold) {
        if (history == null || history.isEmpty()) {
            return null;
        }

        ZonedDateTime beginTime = null;
        for (int i = 0; i < history.size(); i++) {
            ResourceHistoryCsv resourceInfo = history.get(i);
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
    private ZonedDateTime getBeginTimeCpuBelowLower(List<ResourceHistoryCsv> history, Double lowerCpuThreshold) {
        if (history == null || history.isEmpty()) {
            return null;
        }

        ZonedDateTime beginTime = null;
        for (int i = 0; i < history.size(); i++) {
            ResourceHistoryCsv resourceInfo = history.get(i);
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
    private ZonedDateTime getBeginTimeMemoryBelowLower(List<ResourceHistoryCsv> history, Double lowerMemoryThreshold) {
        if (history == null || history.isEmpty()) {
            return null;
        }

        ZonedDateTime beginTime = null;
        for (int i = 0; i < history.size(); i++) {
            ResourceHistoryCsv resourceInfo = history.get(i);
            if (resourceInfo.getMemoryUsage() != null && resourceInfo.getMemoryUsage() < lowerMemoryThreshold) {
                beginTime = ZonedDateTime.parse(resourceInfo.getTimestamp());
            } else {
                return beginTime;
            }
        }
        return beginTime;
    }
}