package com.example.jsonreceiver.repository;

import com.example.jsonreceiver.dto.InstanceTypeLinkCsv;
import com.example.jsoncommon.repository.CsvRepositoryBase;
import org.springframework.stereotype.Repository;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Repository
public class InstanceTypeLinkRepository extends CsvRepositoryBase {

    private static final String FILE_NAME = "InstanceTypeLinkCsv.csv";
    private static final String[] HEADERS = { "ElType", "InstanceTypeId" };

    public void save(String elType, String instanceTypeId) throws IOException {
        writeToCsv(FILE_NAME, HEADERS, elType, instanceTypeId);
    }

    /**
     * ElTypeでインスタンスタイプリンクを検索する
     * 
     * @param elType ElType
     * @return インスタンスタイプリンク情報（存在しない場合はOptional.empty()）
     * @throws IOException IO例外
     */
    public Optional<InstanceTypeLinkCsv> findByElType(String elType) throws IOException {
        List<String> lines = readFromCsv(FILE_NAME);

        if (lines.isEmpty()) {
            return Optional.empty();
        }

        // ヘッダーをスキップして検索
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split(",", -1);
            if (parts.length >= 2 && parts[0].equals(elType)) {
                InstanceTypeLinkCsv link = new InstanceTypeLinkCsv(
                        parts[0], // elType
                        parts[1] // instanceTypeId
                );
                return Optional.of(link);
            }
        }

        return Optional.empty();
    }
}
