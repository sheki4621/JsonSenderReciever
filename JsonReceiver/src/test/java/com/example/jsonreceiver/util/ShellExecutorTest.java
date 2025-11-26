package com.example.jsonreceiver.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ShellExecutorのテストクラス
 */
class ShellExecutorTest {

    private ShellExecutor shellExecutor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        shellExecutor = new ShellExecutor();
    }

    @Test
    void testExecuteShell_成功時に標準出力を返す() throws Exception {
        // テスト用のシェルスクリプトを作成
        Path scriptPath = createTestScript("#!/bin/bash\necho 'Hello World'");

        // シェル実行
        String result = shellExecutor.executeShell(scriptPath.toString(), List.of(), 5);

        // 検証
        assertEquals("Hello World", result.trim());
    }

    @Test
    void testExecuteShell_引数付きで実行成功() throws Exception {
        // 引数をエコーするスクリプト
        Path scriptPath = createTestScript("#!/bin/bash\necho \"$1 $2\"");

        // シェル実行
        String result = shellExecutor.executeShell(
                scriptPath.toString(),
                Arrays.asList("arg1", "arg2"),
                5);

        // 検証
        assertEquals("arg1 arg2", result.trim());
    }

    @Test
    void testExecuteShell_複数行の標準出力を返す() throws Exception {
        // 複数行を出力するスクリプト
        Path scriptPath = createTestScript("#!/bin/bash\necho 'Line1'\necho 'Line2'\necho 'Line3'");

        // シェル実行
        String result = shellExecutor.executeShell(scriptPath.toString(), List.of(), 5);

        // 検証
        assertTrue(result.contains("Line1"));
        assertTrue(result.contains("Line2"));
        assertTrue(result.contains("Line3"));
    }

    @Test
    void testExecuteShell_シェルが非ゼロで終了時は例外をスロー() {
        // エラー終了するスクリプト
        Path scriptPath = createTestScript("#!/bin/bash\nexit 1");

        // 検証
        Exception exception = assertThrows(IOException.class, () -> {
            shellExecutor.executeShell(scriptPath.toString(), List.of(), 5);
        });

        assertTrue(exception.getMessage().contains("非ゼロ") ||
                exception.getMessage().contains("failed") ||
                exception.getMessage().contains("exit code"));
    }

    @Test
    void testExecuteShell_存在しないシェルパスで例外をスロー() {
        // 存在しないパス
        String nonExistentPath = "/path/to/nonexistent/script.sh";

        // 検証
        assertThrows(IOException.class, () -> {
            shellExecutor.executeShell(nonExistentPath, List.of(), 5);
        });
    }

    @Test
    void testExecuteShell_タイムアウト時は例外をスロー() {
        // 長時間実行するスクリプト（10秒スリープ）
        Path scriptPath = createTestScript("#!/bin/bash\nsleep 10");

        // 検証（1秒でタイムアウト）
        assertThrows(TimeoutException.class, () -> {
            shellExecutor.executeShell(scriptPath.toString(), List.of(), 1);
        });
    }

    @Test
    void testExecuteShell_標準エラー出力を含む場合() throws Exception {
        // 標準エラーに出力するスクリプト
        Path scriptPath = createTestScript("#!/bin/bash\necho 'stdout message'\necho 'stderr message' >&2");

        // シェル実行
        String result = shellExecutor.executeShell(scriptPath.toString(), List.of(), 5);

        // 標準出力のみ返されることを確認
        assertTrue(result.contains("stdout message"));
    }

    @Test
    void testExecuteShell_空の引数リストで実行() throws Exception {
        // 引数なしで実行
        Path scriptPath = createTestScript("#!/bin/bash\necho 'No arguments'");

        // シェル実行
        String result = shellExecutor.executeShell(scriptPath.toString(), List.of(), 5);

        // 検証
        assertEquals("No arguments", result.trim());
    }

    /**
     * テスト用のシェルスクリプトファイルを作成
     */
    private Path createTestScript(String content) {
        try {
            Path scriptPath = tempDir.resolve("test_script_" + System.nanoTime() + ".sh");
            Files.writeString(scriptPath, content);

            // 実行権限を付与
            File scriptFile = scriptPath.toFile();
            scriptFile.setExecutable(true);

            return scriptPath;
        } catch (IOException e) {
            throw new RuntimeException("テストスクリプトの作成に失敗しました", e);
        }
    }
}
