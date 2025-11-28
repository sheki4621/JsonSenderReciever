package com.example.jsonreceiver.repository;

import com.example.jsoncommon.repository.CsvRepositoryBase;
import com.example.jsonreceiver.dto.InstanceStatusCsv;
import com.example.jsonreceiver.dto.InstanceStatusValue;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.*;

@Repository
public class InstanceStatusRepository extends CsvRepositoryBase {

    private static final String FILE_NAME = "monitor_target.csv";
    private static final String[] HEADERS = {
            "HOSTNAME", "MACHINE_TYPE", "REGION", "CURRENT_TYPE", "TYPE_ID",
            "TYPE_HIGH", "TYPE_SMALL_STANDARD", "TYPE_MICRO",
            "LASTUPDATE", "AGENT_STATUS", "AGENT_VERSION", "AGENT_LAST_NOTICE_TIME"
    };

    /**
     * インスタンスステータスを保存する（上書き保存）
     * 既存データを読み込み、該当ホスト名のデータを更新または追加してから上書き保存
     * 
     * @param status インスタンスステータス
     * @throws IOException IO例外
     */
    public void save(InstanceStatusCsv status) throws IOException {
        // 既存データを読み込む
        Map<String, InstanceStatusCsv> statusMap = new LinkedHashMap<>();
        List<String> lines = readFromCsv(FILE_NAME);

        if (!lines.isEmpty()) {
            // ヘッダーをスキップして既存データを読み込む
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] parts = line.split(",", -1);
                if (parts.length >= 11) {
                    InstanceStatusCsv existingStatus = new InstanceStatusCsv(
                            parts[0], // hostname
                            parts[1], // machineType
                            parts[2], // region
                            parts[3], // currentType
                            parts[4], // typeId
                            parts[5], // typeHigh
                            parts[6], // typeSmallStandard
                            parts[7], // typeMicro
                            parts[8], // lastUpdate
                            !parts[9].isEmpty() ? InstanceStatusValue.valueOf(parts[9]) : null, // agentStatus
                            parts[10], // agentVersion
                            parts.length >= 12 ? parts[11] : "" // agentLastNoticeTime
                    );
                    statusMap.put(parts[0], existingStatus);
                }
            }
        }

        // 新しいステータスを追加または更新
        statusMap.put(status.getHostname(), status);

        // 全データを上書き保存
        List<Object[]> values = new ArrayList<>();
        for (InstanceStatusCsv s : statusMap.values()) {
            values.add(new Object[] {
                    s.getHostname(),
                    nullToEmpty(s.getMachineType()),
                    nullToEmpty(s.getRegion()),
                    nullToEmpty(s.getCurrentType()),
                    nullToEmpty(s.getTypeId()),
                    nullToEmpty(s.getTypeHigh()),
                    nullToEmpty(s.getTypeSmallStandard()),
                    nullToEmpty(s.getTypeMicro()),
                    nullToEmpty(s.getLastUpdate()),
                    s.getAgentStatus() != null ? s.getAgentStatus().name() : "",
                    nullToEmpty(s.getAgentVersion()),
                    nullToEmpty(s.getAgentLastNoticeTime())
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
    public Optional<InstanceStatusCsv> findByHostname(String hostname) throws IOException {
        List<String> lines = readFromCsv(FILE_NAME);

        if (lines.isEmpty()) {
            return Optional.empty();
        }

        // ヘッダーをスキップして検索
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split(",", -1);
            if (parts.length >= 11 && parts[0].equals(hostname)) {
                InstanceStatusCsv status = new InstanceStatusCsv(
                        parts[0], // hostname
                        parts[1], // machineType
                        parts[2], // region
                        parts[3], // currentType
                        parts[4], // typeId
                        parts[5], // typeHigh
                        parts[6], // typeSmallStandard
                        parts[7], // typeMicro
                        parts[8], // lastUpdate
                        !parts[9].isEmpty() ? InstanceStatusValue.valueOf(parts[9]) : null, // agentStatus
                        parts[10], // agentVersion
                        parts.length >= 12 ? parts[11] : "" // agentLastNoticeTime
                );
                return Optional.of(status);
            }
        }

        return Optional.empty();
    }

    /**
     * 全てのインスタンスステータスを取得する
     * 
     * @return インスタンスステータスのリスト
     * @throws IOException IO例外
     */
    public List<InstanceStatusCsv> findAll() throws IOException {
        List<String> lines = readFromCsv(FILE_NAME);
        List<InstanceStatusCsv> statuses = new ArrayList<>();

        if (lines.isEmpty()) {
            return statuses;
        }

        // ヘッダーをスキップして検索
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split(",", -1);
            if (parts.length >= 11) {
                InstanceStatusCsv status = new InstanceStatusCsv(
                        parts[0], // hostname
                        parts[1], // machineType
                        parts[2], // region
                        parts[3], // currentType
                        parts[4], // typeId
                        parts[5], // typeHigh
                        parts[6], // typeSmallStandard
                        parts[7], // typeMicro
                        parts[8], // lastUpdate
                        !parts[9].isEmpty() ? InstanceStatusValue.valueOf(parts[9]) : null, // agentStatus
                        parts[10], // agentVersion
                        parts.length >= 12 ? parts[11] : "" // agentLastNoticeTime
                );
                statuses.add(status);
            }
        }

        return statuses;
    }

    /**
     * ホスト名でCURRENT_TYPEカラムを更新する
     * 
     * @param hostname    ホスト名
     * @param currentType 現在のインスタンスタイプ
     * @throws IOException IO例外
     */
    public void updateCurrentType(String hostname, String currentType) throws IOException {
        Optional<InstanceStatusCsv> statusOpt = findByHostname(hostname);
        if (statusOpt.isPresent()) {
            InstanceStatusCsv status = statusOpt.get();
            InstanceStatusCsv updatedStatus = new InstanceStatusCsv(
                    status.getHostname(),
                    status.getMachineType(),
                    status.getRegion(),
                    currentType,
                    status.getTypeId(),
                    status.getTypeHigh(),
                    status.getTypeSmallStandard(),
                    status.getTypeMicro(),
                    status.getLastUpdate(),
                    status.getAgentStatus(),
                    status.getAgentVersion(),
                    status.getAgentLastNoticeTime());
            save(updatedStatus);
        }
    }

    /**
     * ホスト名でAGENT_LAST_NOTICE_TIMEカラムを更新する
     * 
     * @param hostname            ホスト名
     * @param agentLastNoticeTime 最終通知受信時刻
     * @throws IOException IO例外
     */
    public void updateAgentLastNoticeTime(String hostname, String agentLastNoticeTime) throws IOException {
        Optional<InstanceStatusCsv> statusOpt = findByHostname(hostname);
        if (statusOpt.isPresent()) {
            InstanceStatusCsv status = statusOpt.get();
            InstanceStatusCsv updatedStatus = new InstanceStatusCsv(
                    status.getHostname(),
                    status.getMachineType(),
                    status.getRegion(),
                    status.getCurrentType(),
                    status.getTypeId(),
                    status.getTypeHigh(),
                    status.getTypeSmallStandard(),
                    status.getTypeMicro(),
                    status.getLastUpdate(),
                    status.getAgentStatus(),
                    status.getAgentVersion(),
                    agentLastNoticeTime);
            save(updatedStatus);
        }
    }

    /**
     * null を空文字列に変換するヘルパーメソッド
     */
    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
