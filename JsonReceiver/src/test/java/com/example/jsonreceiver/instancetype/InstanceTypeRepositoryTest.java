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
 * InstanceTypeRepositoryのテストクラス
 */
class InstanceTypeRepositoryTest {

    private InstanceTypeRepository repository;
    private static final String TEST_OUTPUT_DIR = "./test-csv";
    private static final String FILE_NAME = "InstanceType.csv";

    @BeforeEach
    void setUp() {
        repository = new InstanceTypeRepository();
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
        List<InstanceTypeInfoCsv> instanceTypes = Arrays.asList(
                new InstanceTypeInfoCsv("1", "t2.xlarge", 4, "t2.medium", 2, "t2.micro", 1),
                new InstanceTypeInfoCsv("2", "t3.xlarge", 4, "t3.medium", 2, "t3.micro", 1),
                new InstanceTypeInfoCsv("3", "m5.2xlarge", 8, "m5.large", 2, "m5.small", 1));

        // saveAllメソッドを呼び出し
        repository.saveAll(instanceTypes);

        // ファイルが作成されたことを確認
        Path filePath = Paths.get(TEST_OUTPUT_DIR, FILE_NAME);
        assertTrue(Files.exists(filePath));

        // ファイルの内容を確認
        List<String> lines = Files.readAllLines(filePath);
        assertFalse(lines.isEmpty());

        // ヘッダーが存在することを確認
        assertEquals(
                "InstanceTypeId,HighInstanceType,HighCpuCore,LowInstanceType,LowCpuCore,VeryLowInstanceType,VeryLowCpuCore",
                lines.get(0));

        // データ行数を確認（ヘッダー + 3行）
        assertEquals(4, lines.size());

        // データの内容を確認
        assertTrue(lines.contains("1,t2.xlarge,4,t2.medium,2,t2.micro,1"));
        assertTrue(lines.contains("2,t3.xlarge,4,t3.medium,2,t3.micro,1"));
        assertTrue(lines.contains("3,m5.2xlarge,8,m5.large,2,m5.small,1"));
    }

    @Test
    void testSaveAll_overwritesExistingFile() throws IOException {
        // 最初のデータセットを保存
        List<InstanceTypeInfoCsv> firstSet = Arrays.asList(
                new InstanceTypeInfoCsv("1", "t2.xlarge", 4, "t2.medium", 2, "t2.micro", 1),
                new InstanceTypeInfoCsv("2", "t3.xlarge", 4, "t3.medium", 2, "t3.micro", 1));
        repository.saveAll(firstSet);

        // 2回目のデータセットを保存（上書き）
        List<InstanceTypeInfoCsv> secondSet = Arrays.asList(
                new InstanceTypeInfoCsv("3", "m5.2xlarge", 8, "m5.large", 2, "m5.small", 1),
                new InstanceTypeInfoCsv("4", "c5.4xlarge", 16, "c5.xlarge", 4, "c5.large", 2));
        repository.saveAll(secondSet);

        // ファイルの内容を確認
        Path filePath = Paths.get(TEST_OUTPUT_DIR, FILE_NAME);
        List<String> lines = Files.readAllLines(filePath);

        // 古いデータが存在しないことを確認（上書きされている）
        assertFalse(lines.contains("1,t2.xlarge,4,t2.medium,2,t2.micro,1"));
        assertFalse(lines.contains("2,t3.xlarge,4,t3.medium,2,t3.micro,1"));

        // 新しいデータが存在することを確認
        assertTrue(lines.contains("3,m5.2xlarge,8,m5.large,2,m5.small,1"));
        assertTrue(lines.contains("4,c5.4xlarge,16,c5.xlarge,4,c5.large,2"));

        // データ行数を確認（ヘッダー + 2行）
        assertEquals(3, lines.size());
    }

    @Test
    void testSaveAll_withEmptyList() throws IOException {
        // 空のリストで保存
        List<InstanceTypeInfoCsv> emptyList = Arrays.asList();
        repository.saveAll(emptyList);

        // ファイルが作成されることを確認
        Path filePath = Paths.get(TEST_OUTPUT_DIR, FILE_NAME);
        assertTrue(Files.exists(filePath));

        // ヘッダーのみが存在することを確認
        List<String> lines = Files.readAllLines(filePath);
        assertEquals(1, lines.size());
        assertEquals(
                "InstanceTypeId,HighInstanceType,HighCpuCore,LowInstanceType,LowCpuCore,VeryLowInstanceType,VeryLowCpuCore",
                lines.get(0));
    }

    @Test
    void testSaveAll_createsDirectoryIfNotExists() throws IOException {
        // テストディレクトリを事前に削除
        Path dirPath = Paths.get(TEST_OUTPUT_DIR);
        if (Files.exists(dirPath)) {
            Files.delete(dirPath);
        }

        // データを保存
        List<InstanceTypeInfoCsv> instanceTypes = Arrays.asList(
                new InstanceTypeInfoCsv("1", "t2.xlarge", 4, "t2.medium", 2, "t2.micro", 1));
        repository.saveAll(instanceTypes);

        // ディレクトリとファイルが作成されたことを確認
        assertTrue(Files.exists(dirPath));
        assertTrue(Files.isDirectory(dirPath));

        Path filePath = dirPath.resolve(FILE_NAME);
        assertTrue(Files.exists(filePath));
    }
}
