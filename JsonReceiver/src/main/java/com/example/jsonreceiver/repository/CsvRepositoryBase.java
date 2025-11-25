package com.example.jsonreceiver.repository;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class CsvRepositoryBase {

    @Value("${app.csv.output-dir}")
    private String outputDir;

    protected void writeToCsv(String fileName, String[] headers, Object... values) throws IOException {
        Path dirPath = Paths.get(outputDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        Path filePath = dirPath.resolve(fileName);
        boolean fileExists = Files.exists(filePath);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .setSkipHeaderRecord(fileExists)
                .build();

        try (FileWriter writer = new FileWriter(filePath.toFile(), true);
                CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(values);
        }
    }
}
