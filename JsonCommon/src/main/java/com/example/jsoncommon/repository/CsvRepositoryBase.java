package com.example.jsoncommon.repository;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public abstract class CsvRepositoryBase {

    @Value("${app.csv.output-dir}")
    private String outputDir;

    /**
     * outputDirを設定するセッター
     * 
     * @param outputDir 出力ディレクトリ
     */
    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * CSV ファイルに追記モードでデータを書き込む
     * 
     * @param fileName ファイル名
     * @param headers  ヘッダー配列
     * @param values   書き込む値
     * @throws IOException IO例外
     */
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

    /**
     * CSV ファイルに上書きモードでデータを書き込む
     * 既存ファイルが存在する場合は削除してから新規作成する
     * 
     * @param fileName ファイル名
     * @param headers  ヘッダー配列
     * @param values   書き込む値のリスト
     * @throws IOException IO例外
     */
    protected void overwriteToCsv(String fileName, String[] headers, List<Object[]> values) throws IOException {
        Path dirPath = Paths.get(outputDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        Path filePath = dirPath.resolve(fileName);

        // 既存ファイルが存在する場合は削除
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .build();

        try (FileWriter writer = new FileWriter(filePath.toFile(), false);
                CSVPrinter printer = new CSVPrinter(writer, format)) {
            for (Object[] valueArray : values) {
                printer.printRecord(valueArray);
            }
        }
    }

    /**
     * CSV ファイルからデータを読み込む
     * 
     * @param fileName ファイル名
     * @return CSVファイルの全行（ヘッダーを含む）
     * @throws IOException IO例外
     */
    protected List<String> readFromCsv(String fileName) throws IOException {
        Path dirPath = Paths.get(outputDir);
        Path filePath = dirPath.resolve(fileName);

        if (!Files.exists(filePath)) {
            return List.of();
        }

        return Files.readAllLines(filePath);
    }
}
