package com.example.jsonreceiver.repository;

import org.springframework.stereotype.Repository;
import java.io.IOException;

@Repository
public class InstanceTypeLinkRepository extends CsvRepositoryBase {

    private static final String FILE_NAME = "InstanceTypeLink.csv";
    private static final String[] HEADERS = { "ElType", "Type", "InstanceType", "InstanceTypeId" };

    public void save(String elType, String type, String instanceType, String instanceTypeId) throws IOException {
        writeToCsv(FILE_NAME, HEADERS, elType, type, instanceType, instanceTypeId);
    }
}
