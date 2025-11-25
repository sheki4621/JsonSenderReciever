package com.example.jsonreceiver.repository;

import org.springframework.stereotype.Repository;
import java.io.IOException;

@Repository
public class ResourceInfoRepository extends CsvRepositoryBase {

    private static final String FILE_NAME = "ResourceInfo.csv";
    private static final String[] HEADERS = { "Hostname", "Timestamp", "CpuUsage", "MemoryUsage" };

    public void save(String hostname, String timestamp, String cpuUsage, String memoryUsage) throws IOException {
        writeToCsv(FILE_NAME, HEADERS, hostname, timestamp, cpuUsage, memoryUsage);
    }
}
