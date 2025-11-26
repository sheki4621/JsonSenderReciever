package com.example.jsonreceiver.repository;

import com.example.jsoncommon.dto.MetricsJson;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Repository;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Repository
public class CsvRepository {

    private static final String CSV_DIR = "csv";
    private static final String RESOURCE_INFO_CSV = "ResourceInfo.csv";
    private static final String[] HEADERS = { "Hostname", "Timestamp", "CpuUsage", "MemoryUsage" };

    public void saveResourceInfo(MetricsJson metricsJson) throws IOException {
        Path dirPath = Paths.get(CSV_DIR);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        Path filePath = dirPath.resolve(RESOURCE_INFO_CSV);
        boolean fileExists = Files.exists(filePath);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(HEADERS)
                .setSkipHeaderRecord(fileExists)
                .build();

        try (FileWriter writer = new FileWriter(filePath.toFile(), true);
                CSVPrinter printer = new CSVPrinter(writer, format)) {

            printer.printRecord(
                    metricsJson.getInstanceName(),
                    metricsJson.getTimestamp(),
                    metricsJson.getMetrics().getCpuUsage(),
                    metricsJson.getMetrics().getMemoryUsage());
        }
    }
}
