package com.example.jsonreceiver.repository;

import com.example.jsonreceiver.dto.SystemInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SystemInfoRepositoryのテストクラス
 */
class SystemInfoRepositoryTest {

    private SystemInfoRepository repository;
    private static final String TEST_OUTPUT_DIR = "./test-csv";
    private static final String FILE_NAME = "SystemInfo.csv";

    @BeforeEach
    void setUp() {
        repository = new SystemInfoRepository();
        // テスト用の出力ディレクトリを設定
        ReflectionTestUtils.setField(repository, "outputDir", TEST_OUTPUT_DIR);
    }

    @AfterEach
    void tearDown() throws IOException {
        // テスト後にテストディレクトリとファイルを削除
        Path filePath = Paths.get(TEST_OUTPUT_DIR, FILE_NAME);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
        Path dirPath = Paths.get(TEST_OUTPUT_DIR);
        if (Files.exists(dirPath)) {
            Files.delete(dirPath);
        }
    }

    @Test
    void testSaveAll_createsFileWithHeaders() throws IOException {
        // テストデータを作成
        List<SystemInfo> systemInfoList = Arrays.asList(
                new SystemInfo("192.168.1.10", "server01.example.com", "RHEL", "HEL-PROD-01"),
                new SystemInfo("192.168.1.20", "server02.example.com", "CentOS", "HEL-PROD-02"),
                new SystemInfo("192.168.1.30", "server03.example.com", "Ubuntu", "HEL-DEV-01"));

        // saveAllメソッドを呼び出し
        repository.saveAll(systemInfoList);

        // ファイルが作成されたことを確認
        Path filePath = Paths.get(TEST_OUTPUT_DIR, FILE_NAME);
        assertTrue(Files.exists(filePath));

        // ファイルの内容を確認
        List<String> lines = Files.readAllLines(filePath);
        assertFalse(lines.isEmpty());

        // ヘッダーが存在することを確認
        assertEquals("Ip,Hostname,ElType,HelName", lines.get(0));

        // データ行数を確認（ヘッダー + 3行）
        assertEquals(4, lines.size());

        // データの内容を確認
        assertTrue(lines.stream().anyMatch(line -> line.contains("192.168.1.10")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("server01.example.com")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("HEL-PROD-01")));
    }

    @Test
    void testSaveAll_overwritesExistingFile() throws IOException {
        // 最初のデータセットを保存
        List<SystemInfo> firstSet = Arrays.asList(
                new SystemInfo("192.168.1.10", "server01.example.com", "RHEL", "HEL-PROD-01"),
                new SystemInfo("192.168.1.20", "server02.example.com", "CentOS", "HEL-PROD-02"));
        repository.saveAll(firstSet);

        // 2回目のデータセットを保存（上書き）
        List<SystemInfo> secondSet = Arrays.asList(
                new SystemInfo("10.0.0.1", "newserver.example.com", "Ubuntu", "HEL-TEST-01"));
        repository.saveAll(secondSet);

        // ファイルの内容を確認
        Path filePath = Paths.get(TEST_OUTPUT_DIR, FILE_NAME);
        List<String> lines = Files.readAllLines(filePath);

        // 古いデータが存在しないことを確認（上書きされている）
        assertFalse(lines.stream().anyMatch(line -> line.contains("192.168.1.10")));
        assertFalse(lines.stream().anyMatch(line -> line.contains("server01.example.com")));

        // 新しいデータが存在することを確認
        assertTrue(lines.stream().anyMatch(line -> line.contains("10.0.0.1")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("newserver.example.com")));

        // データ行数を確認（ヘッダー + 1行）
        assertEquals(2, lines.size());
    }

    @Test
    void testSaveAll_withEmptyList() throws IOException {
        // 空のリストで保存
        List<SystemInfo> emptyList = Arrays.asList();
        repository.saveAll(emptyList);

        // ファイルが作成されることを確認
        Path filePath = Paths.get(TEST_OUTPUT_DIR, FILE_NAME);
        assertTrue(Files.exists(filePath));

        // ヘッダーのみが存在することを確認
        List<String> lines = Files.readAllLines(filePath);
        assertEquals(1, lines.size());
        assertEquals("Ip,Hostname,ElType,HelName", lines.get(0));
    }

    @Test
    void testSaveAll_createsDirectoryIfNotExists() throws IOException {
        // テストディレクトリを事前に削除
        Path dirPath = Paths.get(TEST_OUTPUT_DIR);
        if (Files.exists(dirPath)) {
            Files.delete(dirPath);
        }

        // データを保存
        List<SystemInfo> systemInfoList = Arrays.asList(
                new SystemInfo("192.168.1.10", "server01.example.com", "RHEL", "HEL-PROD-01"));
        repository.saveAll(systemInfoList);

        // ディレクトリとファイルが作成されたことを確認
        assertTrue(Files.exists(dirPath));
        assertTrue(Files.isDirectory(dirPath));

        Path filePath = dirPath.resolve(FILE_NAME);
        assertTrue(Files.exists(filePath));
    }
}
