package com.example.jsonreceiver.repository;

import com.example.jsonreceiver.dto.AllInstanceCsv;
import com.example.jsoncommon.repository.CsvRepositoryBase;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AllInstanceRepository extends CsvRepositoryBase {

    private static final String FILE_NAME = "all_instance.csv";
    private static final String[] HEADERS = { "HOSTNAME", "MACHINE_TYPE", "GROUP_NAME" };

    /**
     * 単一のインスタンス情報を追記保存する
     * 
     * @param hostname    ホスト名
     * @param machineType 装置タイプ
     * @param groupName   グループ名
     * @throws IOException IO例外
     */
    public void save(String hostname, String machineType, String groupName) throws IOException {
        writeToCsv(FILE_NAME, HEADERS, hostname, machineType, groupName);
    }

    /**
     * インスタンス情報のリストを上書き保存する
     * 既存のファイルが存在する場合は削除してから新規作成する
     * 
     * @param allInstanceList インスタンス情報のリスト
     * @throws IOException IO例外
     */
    public void saveAll(List<AllInstanceCsv> allInstanceList) throws IOException {
        List<Object[]> values = new ArrayList<>();
        for (AllInstanceCsv info : allInstanceList) {
            values.add(new Object[] {
                    info.getHostname(),
                    info.getMachineType(),
                    info.getGroupName()
            });
        }
        overwriteToCsv(FILE_NAME, HEADERS, values);
    }

    /**
     * ホスト名でインスタンス情報を検索する
     * 
     * @param hostname ホスト名
     * @return インスタンス情報（存在しない場合はOptional.empty()）
     * @throws IOException IO例外
     */
    public Optional<AllInstanceCsv> findByHostname(String hostname) throws IOException {
        List<String> lines = readFromCsv(FILE_NAME);

        if (lines.isEmpty()) {
            return Optional.empty();
        }

        // ヘッダーをスキップして検索
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split(",", -1);
            if (parts.length >= 3 && parts[0].equals(hostname)) {
                AllInstanceCsv info = new AllInstanceCsv(
                        parts[0], // hostname
                        parts[1], // machineType
                        parts[2] // groupName
                );
                return Optional.of(info);
            }
        }

        return Optional.empty();
    }
}
