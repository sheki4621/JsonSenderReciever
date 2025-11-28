package com.example.jsonreceiver.repository;

import com.example.jsonreceiver.dto.InstanceTypeInfoCsv;
import com.example.jsoncommon.repository.CsvRepositoryBase;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class InstanceTypeRepository extends CsvRepositoryBase {

    private static final String FILE_NAME = "InstanceType.csv";
    private static final String[] HEADERS = {
            "InstanceTypeId", "HighInstanceType", "HighCpuCore",
            "LowInstanceType", "LowCpuCore", "VeryLowInstanceType", "VeryLowCpuCore"
    };

    /**
     * インスタンスタイプのリストを上書き保存する
     * 既存のファイルが存在する場合は削除してから新規作成する
     * 
     * @param instanceTypes インスタンスタイプ情報のリスト
     * @throws IOException IO例外
     */
    public void saveAll(List<InstanceTypeInfoCsv> instanceTypes) throws IOException {
        List<Object[]> values = new ArrayList<>();
        for (InstanceTypeInfoCsv info : instanceTypes) {
            values.add(new Object[] {
                    info.getInstanceTypeId(),
                    info.getHighInstanceType(),
                    info.getHighCpuCore(),
                    info.getLowInstanceType(),
                    info.getLowCpuCore(),
                    info.getVeryLowInstanceType(),
                    info.getVeryLowCpuCore()
            });
        }
        overwriteToCsv(FILE_NAME, HEADERS, values);
    }

    /**
     * InstanceTypeIdでインスタンスタイプ情報を検索する
     * 
     * @param instanceTypeId インスタンスタイプID
     * @return インスタンスタイプ情報（存在しない場合はOptional.empty()）
     * @throws IOException IO例外
     */
    public Optional<InstanceTypeInfoCsv> findByInstanceTypeId(String instanceTypeId) throws IOException {
        List<String> lines = readFromCsv(FILE_NAME);

        if (lines.isEmpty()) {
            return Optional.empty();
        }

        // ヘッダーをスキップして検索
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split(",", -1);
            if (parts.length >= 7 && parts[0].equals(instanceTypeId)) {
                InstanceTypeInfoCsv info = new InstanceTypeInfoCsv(
                        parts[0], // instanceTypeId
                        parts[1], // highInstanceType
                        Integer.parseInt(parts[2]), // highCpuCore
                        parts[3], // lowInstanceType
                        Integer.parseInt(parts[4]), // lowCpuCore
                        parts[5], // veryLowInstanceType
                        Integer.parseInt(parts[6]) // veryLowCpuCore
                );
                return Optional.of(info);
            }
        }

        return Optional.empty();
    }

}
