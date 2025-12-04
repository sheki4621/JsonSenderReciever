package com.example.jsonsender;

import com.example.jsonsender.config.AppConfig;
import com.example.jsonsender.tcp.TcpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileManagerTest {

    @TempDir
    Path tempDir;

    @Mock
    private TcpClient tcpClient;

    private JsonFileManager jsonFileManager;
    private AppConfig appConfig;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        appConfig = new AppConfig();
        appConfig.setTimezone("Asia/Tokyo");

        AppConfig.Json jsonConfig = new AppConfig.Json();
        jsonConfig.setOutputDir(tempDir.toString());
        jsonConfig.setRotationDay(7);
        appConfig.setJson(jsonConfig);

        AppConfig.Dist distConfig = new AppConfig.Dist();
        distConfig.setHostname("localhost");
        distConfig.setPort(8080);
        appConfig.setDist(distConfig);

        objectMapper = new ObjectMapper();
        jsonFileManager = new JsonFileManager(appConfig, tcpClient, objectMapper);
    }

    @Test
    void testSaveWithJapaneseCharacters_EUC_JP() throws IOException, InterruptedException {
        // Arrange
        Map<String, Object> data = new HashMap<>();
        data.put("NoticeType", "METRICS");
        data.put("Hostname", "テストホスト");
        data.put("Message", "日本語メッセージ");

        // Act
        jsonFileManager.save(data);

        // Wait a bit for the file to be written
        Thread.sleep(100);

        // Assert
        File[] files = tempDir.toFile().listFiles((d, name) -> name.endsWith(".json"));
        assertNotNull(files, "JSON files should be created");
        assertTrue(files.length > 0, "At least one JSON file should exist");

        File jsonFile = files[0];
        assertTrue(jsonFile.exists(), "JSON file should exist");

        // Verify encoding is EUC-JP by checking bytes
        byte[] bytes = Files.readAllBytes(jsonFile.toPath());

        // "日" in EUC-JP is 0xC6 0xFC
        boolean containsEucJpChar = false;
        for (int i = 0; i < bytes.length - 1; i++) {
            if ((bytes[i] == (byte) 0xC6) && (bytes[i + 1] == (byte) 0xFC)) {
                containsEucJpChar = true;
                break;
            }
        }

        assertTrue(containsEucJpChar,
                "File should contain EUC-JP bytes for '日' (0xC6 0xFC)");

        // Verify we can read it back as EUC-JP
        String content = new String(bytes, "EUC-JP");
        assertTrue(content.contains("日本語メッセージ"),
                "Content should contain Japanese text when read as EUC-JP");
        assertTrue(content.contains("テストホスト"),
                "Content should contain Japanese hostname when read as EUC-JP");
    }

    @Test
    void testSaveCreatesOutputDirectory() {
        // Arrange
        Path newDir = tempDir.resolve("new_json_dir");
        appConfig.getJson().setOutputDir(newDir.toString());

        Map<String, Object> data = new HashMap<>();
        data.put("NoticeType", "TEST");
        data.put("Message", "test");

        // Act
        jsonFileManager.save(data);

        // Assert
        assertTrue(Files.exists(newDir), "Output directory should be created");
        assertTrue(Files.isDirectory(newDir), "Output directory should be a directory");
    }
}
