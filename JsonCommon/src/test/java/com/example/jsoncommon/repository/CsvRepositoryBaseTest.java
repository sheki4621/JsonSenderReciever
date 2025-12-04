package com.example.jsoncommon.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CsvRepositoryBaseTest {

    @TempDir
    Path tempDir;

    private TestCsvRepository repository;
    private Path csvDir;

    // Concrete implementation for testing
    private static class TestCsvRepository extends CsvRepositoryBase {
        public void write(String fileName, String[] headers, Object... values) throws IOException {
            writeToCsv(fileName, headers, values);
        }

        public List<String> read(String fileName) throws IOException {
            return readFromCsv(fileName);
        }
    }

    @BeforeEach
    public void setUp() throws IOException {
        csvDir = tempDir.resolve("csv");
        Files.createDirectories(csvDir);
        repository = new TestCsvRepository();
        repository.setOutputDir(csvDir.toString());
    }

    @Test
    public void testWriteAndReadWithJapaneseCharacters_EUC_JP() throws IOException {
        String fileName = "test_japanese.csv";
        String[] headers = { "Header1", "Header2" };
        String japaneseValue = "日本語テスト";

        // Write
        repository.write(fileName, headers, "Value1", japaneseValue);

        // Verify file exists
        Path filePath = csvDir.resolve(fileName);
        assertTrue(Files.exists(filePath));

        // Verify encoding is EUC-JP by checking bytes
        byte[] bytes = Files.readAllBytes(filePath);

        // Check if we can decode it as EUC-JP correctly
        String contentEucJp = new String(bytes, "EUC-JP");
        assertTrue(contentEucJp.contains(japaneseValue),
                "File content should contain the Japanese string when read as EUC-JP");

        // Let's be more specific about bytes to ensure it IS EUC-JP.
        // "日" in EUC-JP is 0xC6 0xFC.
        // We check for the presence of EUC-JP bytes for "日".
        boolean containsEucJpHi = false;
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X ", b));
        }
        System.out.println("File content (hex): " + hexString.toString());

        for (int i = 0; i < bytes.length - 1; i++) {
            if ((bytes[i] == (byte) 0xC6) && (bytes[i + 1] == (byte) 0xFC)) {
                containsEucJpHi = true;
                break;
            }
        }
        assertTrue(containsEucJpHi,
                "File should contain EUC-JP bytes for '日' (0xC6 0xFC). Actual bytes: " + hexString.toString());

        // Read back using repository
        List<String> lines = repository.read(fileName);
        boolean foundInLines = false;
        for (String line : lines) {
            if (line.contains(japaneseValue)) {
                foundInLines = true;
                break;
            }
        }
        assertTrue(foundInLines, "Repository should be able to read back the Japanese value correctly");
    }
}
