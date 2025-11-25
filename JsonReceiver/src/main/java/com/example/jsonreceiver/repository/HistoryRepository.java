package com.example.jsonreceiver.repository;

import org.springframework.stereotype.Repository;
import java.io.IOException;

@Repository
public class HistoryRepository extends CsvRepositoryBase {

    private static final String FILE_NAME = "History.csv";
    private static final String[] HEADERS = { "Hostname", "Timestamp", "NoticeId", "AdditionalInfo" };

    public void save(String hostname, String timestamp, String noticeId, String additionalInfo) throws IOException {
        writeToCsv(FILE_NAME, HEADERS, hostname, timestamp, noticeId, additionalInfo);
    }
}
