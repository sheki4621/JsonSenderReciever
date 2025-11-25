package com.example.jsonreceiver.repository;

import org.springframework.stereotype.Repository;
import java.io.IOException;

@Repository
public class InstanceStatusRepository extends CsvRepositoryBase {

    private static final String FILE_NAME = "InstanceStatus.csv";
    private static final String[] HEADERS = { "Hostname", "Status", "Timestamp" };

    public void save(String hostname, String status, String timestamp) throws IOException {
        writeToCsv(FILE_NAME, HEADERS, hostname, status, timestamp);
    }
}
