package com.example.jsonreceiver.repository;

import org.springframework.stereotype.Repository;
import java.io.IOException;

@Repository
public class InstanceTypeRepository extends CsvRepositoryBase {

    private static final String FILE_NAME = "InstanceType.csv";
    private static final String[] HEADERS = { "Id", "InstanceType" };

    public void save(String id, String instanceType) throws IOException {
        writeToCsv(FILE_NAME, HEADERS, id, instanceType);
    }
}
