package com.example.jsonreceiver.repository;

import org.springframework.stereotype.Repository;
import java.io.IOException;

@Repository
public class SystemInfoRepository extends CsvRepositoryBase {

    private static final String FILE_NAME = "SystemInfo.csv";
    private static final String[] HEADERS = { "Ip", "Hostname", "ElType", "HelName" };

    public void save(String ip, String hostname, String elType, String helName) throws IOException {
        writeToCsv(FILE_NAME, HEADERS, ip, hostname, elType, helName);
    }
}
