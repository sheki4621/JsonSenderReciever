package com.example.jsonreceiver.repository;

import com.example.jsonreceiver.dto.InstanceStatus;
import com.example.jsonreceiver.dto.InstanceStatusValue;
import com.example.jsonreceiver.dto.InstanceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.*;

@Repository
public class InstanceStatusRepository extends CsvRepositoryBase {

    private static final Logger logger = LoggerFactory.getLogger(InstanceStatusRepository.class);
    private static final String FILE_NAME = "InstanceStatus.csv";
    private static final String[] HEADERS = { "Hostname", "Status", "IsInstalled", "AgentVersion", "Timestamp",
            "InstanceType" };

    /**
     * インスタンスステータスを保存する（上書き保存）
     * 既存データを読み込み、該当ホスト名のデータを更新または追加してから上書き保存
     * 
     * @param status インスタンスステータス
     * @throws IOException IO例外
     */
    public void save(InstanceStatus status) throws IOException {
        // 既存データを読み込む
        Map<String, InstanceStatus> statusMap = new LinkedHashMap<>();
        List<String> lines = readFromCsv(FILE_NAME);

        if (!lines.isEmpty()) {
            // ヘッダーをスキップして既存データを読み込む
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] parts = line.split(",", -1);
                if (parts.length >= 6) {
                    String hostname = parts[0];
                    InstanceType instanceType = null;
                    if (!parts[5].isEmpty()) {
                        try {
                            instanceType = InstanceType.valueOf(parts[5]);
                        } catch (IllegalArgumentException e) {
                            logger.warn("無効な InstanceType の値: {}", parts[5]);
                        }
                    }
                    InstanceStatus existingStatus = new InstanceStatus(
                            hostname,
                            InstanceStatusValue.valueOf(parts[1]),
                            Boolean.parseBoolean(parts[2]),
                            parts[3],
                            parts[4],
                            instanceType);
                    statusMap.put(hostname, existingStatus);
                }
            }
        }

        // 新しいステータスを追加または更新
        statusMap.put(status.getHostname(), status);

        // 全データを上書き保存
        List<Object[]> values = new ArrayList<>();
        for (InstanceStatus s : statusMap.values()) {
            values.add(new Object[] {
                    s.getHostname(),
                    s.getStatus().name(),
                    s.getIsInstalled(),
                    s.getAgentVersion(),
                    s.getTimestamp(),
                    s.getInstanceType() != null ? s.getInstanceType().name() : ""
            });
        }

        overwriteToCsv(FILE_NAME, HEADERS, values);
    }

    /**
     * ホスト名でインスタンスステータスを検索する
     * 
     * @param hostname ホスト名
     * @return インスタンスステータス（存在しない場合はOptional.empty()）
     * @throws IOException IO例外
     */
    public Optional<InstanceStatus> findByHostname(String hostname) throws IOException {
        List<String> lines = readFromCsv(FILE_NAME);

        if (lines.isEmpty()) {
            return Optional.empty();
        }

        // ヘッダーをスキップして検索
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split(",", -1);
            if (parts.length >= 6 && parts[0].equals(hostname)) {
                InstanceType instanceType = null;
                if (!parts[5].isEmpty()) {
                    try {
                        instanceType = InstanceType.valueOf(parts[5]);
                    } catch (IllegalArgumentException e) {
                        logger.warn("無効な InstanceType の値: {}", parts[5]);
                    }
                }
                InstanceStatus status = new InstanceStatus(
                        parts[0],
                        InstanceStatusValue.valueOf(parts[1]),
                        Boolean.parseBoolean(parts[2]),
                        parts[3],
                        parts[4],
                        instanceType);
                return Optional.of(status);
            }
        }

        return Optional.empty();
    }

    /**
     * ホスト名でInstanceTypeカラムを更新する
     * 
     * @param hostname     ホスト名
     * @param instanceType インスタンスタイプ
     * @throws IOException IO例外
     */
    public void updateInstanceType(String hostname, InstanceType instanceType) throws IOException {
        List<String> lines = readFromCsv(FILE_NAME);

        if (lines.isEmpty()) {
            return;
        }

        Map<String, InstanceStatus> statusMap = new LinkedHashMap<>();

        // 既存データを読み込む
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split(",", -1);
            if (parts.length >= 6) {
                String hostnameInCsv = parts[0];
                InstanceType existingInstanceType = null;
                if (!parts[5].isEmpty()) {
                    try {
                        existingInstanceType = InstanceType.valueOf(parts[5]);
                    } catch (IllegalArgumentException e) {
                        logger.warn("無効な InstanceType の値: {}", parts[5]);
                    }
                }
                InstanceStatus existingStatus = new InstanceStatus(
                        hostnameInCsv,
                        InstanceStatusValue.valueOf(parts[1]),
                        Boolean.parseBoolean(parts[2]),
                        parts[3],
                        parts[4],
                        existingInstanceType);
                statusMap.put(hostnameInCsv, existingStatus);
            }
        }

        // 指定されたホスト名のInstanceTypeを更新
        if (statusMap.containsKey(hostname)) {
            InstanceStatus currentStatus = statusMap.get(hostname);
            InstanceStatus updatedStatus = new InstanceStatus(
                    currentStatus.getHostname(),
                    currentStatus.getStatus(),
                    currentStatus.getIsInstalled(),
                    currentStatus.getAgentVersion(),
                    currentStatus.getTimestamp(),
                    instanceType);
            statusMap.put(hostname, updatedStatus);

            // 全データを上書き保存
            List<Object[]> values = new ArrayList<>();
            for (InstanceStatus s : statusMap.values()) {
                values.add(new Object[] {
                        s.getHostname(),
                        s.getStatus().name(),
                        s.getIsInstalled(),
                        s.getAgentVersion(),
                        s.getTimestamp(),
                        s.getInstanceType() != null ? s.getInstanceType().name() : ""
                });
            }

            overwriteToCsv(FILE_NAME, HEADERS, values);
        }
    }
}
