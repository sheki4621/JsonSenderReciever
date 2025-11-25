package com.example.jsonreceiver.repository;

import com.example.jsonreceiver.dto.SystemInfo;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class SystemInfoRepository extends CsvRepositoryBase {

    private static final String FILE_NAME = "SystemInfo.csv";
    private static final String[] HEADERS = { "Ip", "Hostname", "ElType", "HelName" };

    /**
     * 単一のシステム情報を追記保存する
     * 
     * @param ip       IPアドレス
     * @param hostname ホスト名
     * @param elType   EL種別
     * @param helName  HEL名
     * @throws IOException IO例外
     */
    public void save(String ip, String hostname, String elType, String helName) throws IOException {
        writeToCsv(FILE_NAME, HEADERS, ip, hostname, elType, helName);
    }

    /**
     * システム情報のリストを上書き保存する
     * 既存のファイルが存在する場合は削除してから新規作成する
     * 
     * @param systemInfoList システム情報のリスト
     * @throws IOException IO例外
     */
    public void saveAll(List<SystemInfo> systemInfoList) throws IOException {
        List<Object[]> values = new ArrayList<>();
        for (SystemInfo info : systemInfoList) {
            values.add(new Object[] {
                    info.getIpAddress(),
                    info.getHostname(),
                    info.getElType(),
                    info.getHelName()
            });
        }
        overwriteToCsv(FILE_NAME, HEADERS, values);
    }

    /**
     * ホスト名でシステム情報を検索する
     * 
     * @param hostname ホスト名
     * @return システム情報（存在しない場合はOptional.empty()）
     * @throws IOException IO例外
     */
    public Optional<SystemInfo> findByHostname(String hostname) throws IOException {
        List<String> lines = readFromCsv(FILE_NAME);

        if (lines.isEmpty()) {
            return Optional.empty();
        }

        // ヘッダーをスキップして検索
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split(",", -1);
            if (parts.length >= 4 && parts[1].equals(hostname)) {
                SystemInfo info = new SystemInfo(
                        parts[0], // ipAddress
                        parts[1], // hostname
                        parts[2], // elType
                        parts[3] // helName
                );
                return Optional.of(info);
            }
        }

        return Optional.empty();
    }
}
