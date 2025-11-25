package com.example.jsonreceiver.repository;

import com.example.jsonreceiver.dto.InstanceTypeInfo;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class InstanceTypeRepository extends CsvRepositoryBase {

    private static final String FILE_NAME = "InstanceType.csv";
    private static final String[] HEADERS = { "Id", "InstanceType" };

    /**
     * 単一のインスタンスタイプを追記保存する
     * 
     * @param id           ID
     * @param instanceType インスタンスタイプ名
     * @throws IOException IO例外
     */
    public void save(String id, String instanceType) throws IOException {
        writeToCsv(FILE_NAME, HEADERS, id, instanceType);
    }

    /**
     * インスタンスタイプのリストを上書き保存する
     * 既存のファイルが存在する場合は削除してから新規作成する
     * 
     * @param instanceTypes インスタンスタイプ情報のリスト
     * @throws IOException IO例外
     */
    public void saveAll(List<InstanceTypeInfo> instanceTypes) throws IOException {
        List<Object[]> values = new ArrayList<>();
        for (InstanceTypeInfo info : instanceTypes) {
            values.add(new Object[] { info.getId(), info.getInstanceType() });
        }
        overwriteToCsv(FILE_NAME, HEADERS, values);
    }

}
