package com.example.jsonreceiver.instancetype;

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
 * AllInstanceRepositoryのテストクラス
 */
class AllInstanceRepositoryTest {

    private AllInstanceRepository repository;
    private static final String TEST_OUTPUT_DIR = "./test-csv";
    private static final String FILE_NAME = "all_instance.csv";

    @BeforeEach
    void setUp() {
        repository = new AllInstanceRepository();
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
        List<AllInstanceCsv> allInstanceList = Arrays.asList(
                new AllInstanceCsv("server01.example.com", "ECS", "GROUP-A"),
                new AllInstanceCsv("server02.example.com", "EDB", "GROUP-B"),
                new AllInstanceCsv("server03.example.com", "ECS", "GROUP-A"));

        // saveAllメソッドを呼び出し
        repository.saveAll(allInstanceList);

        // ファイルが作成されたことを確認
        Path filePath = Paths.get(TEST_OUTPUT_DIR, FILE_NAME);
        assertTrue(Files.exists(filePath));

        // ファイルの内容を確認
        List<String> lines = Files.readAllLines(filePath, java.nio.charset.Charset.forName("EUC-JP"));
        assertFalse(lines.isEmpty());

        // ヘッダーが存在することを確認
        assertEquals("HOSTNAME,MACHINE_TYPE,GROUP_NAME", lines.get(0));

        // データ行数を確認（ヘッダー + 3行）
        assertEquals(4, lines.size());

        // データの内容を確認
        assertTrue(lines.stream().anyMatch(line -> line.contains("server01.example.com")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("ECS")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("GROUP-A")));
    }

    @Test
    void testSaveAll_overwritesExistingFile() throws IOException {
        // 最初のデータセットを保存
        List<AllInstanceCsv> firstSet = Arrays.asList(
                new AllInstanceCsv("server01.example.com", "ECS", "GROUP-A"),
                new AllInstanceCsv("server02.example.com", "EDB", "GROUP-B"));
        repository.saveAll(firstSet);

        // 2回目のデータセットを保存（上書き）
        List<AllInstanceCsv> secondSet = Arrays.asList(
                new AllInstanceCsv("newserver.example.com", "ECS", "GROUP-C"));
        repository.saveAll(secondSet);

        // ファイルの内容を確認
        Path filePath = Paths.get(TEST_OUTPUT_DIR, FILE_NAME);
        List<String> lines = Files.readAllLines(filePath, java.nio.charset.Charset.forName("EUC-JP"));

        // 古いデータが存在しないことを確認（上書きされている）
        assertFalse(lines.stream().anyMatch(line -> line.contains("server01.example.com")));
        assertFalse(lines.stream().anyMatch(line -> line.contains("GROUP-B")));

        // 新しいデータが存在することを確認
        assertTrue(lines.stream().anyMatch(line -> line.contains("newserver.example.com")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("GROUP-C")));

        // データ行数を確認（ヘッダー + 1行）
        assertEquals(2, lines.size());
    }

    @Test
    void testSaveAll_withEmptyList() throws IOException {
        // 空のリストで保存
        List<AllInstanceCsv> emptyList = Arrays.asList();
        repository.saveAll(emptyList);

        // ファイルが作成されることを確認
        Path filePath = Paths.get(TEST_OUTPUT_DIR, FILE_NAME);
        assertTrue(Files.exists(filePath));

        // ヘッダーのみが存在することを確認
        List<String> lines = Files.readAllLines(filePath, java.nio.charset.Charset.forName("EUC-JP"));
        assertEquals(1, lines.size());
        assertEquals("HOSTNAME,MACHINE_TYPE,GROUP_NAME", lines.get(0));
    }

    @Test
    void testSaveAll_createsDirectoryIfNotExists() throws IOException {
        // テストディレクトリを事前に削除
        Path dirPath = Paths.get(TEST_OUTPUT_DIR);
        if (Files.exists(dirPath)) {
            Files.delete(dirPath);
        }

        // データを保存
        List<AllInstanceCsv> allInstanceList = Arrays.asList(
                new AllInstanceCsv("server01.example.com", "ECS", "GROUP-A"));
        repository.saveAll(allInstanceList);

        // ディレクトリとファイルが作成されたことを確認
        assertTrue(Files.exists(dirPath));
        assertTrue(Files.isDirectory(dirPath));

        Path filePath = dirPath.resolve(FILE_NAME);
        assertTrue(Files.exists(filePath));
    }
}
