package com.example.jsoncommon.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 外部コマンド・スクリプトを実行するユーティリティクラス
 */
@Component
public class CommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CommandExecutor.class);

    /**
     * 外部コマンド・スクリプトを実行し、標準出力を返します
     *
     * @param commandPath    コマンド・スクリプトのパス
     * @param args           コマンドに渡す引数のリスト
     * @param timeoutSeconds タイムアウト時間（秒）
     * @return コマンドの標準出力
     * @throws IOException          コマンド実行に失敗した場合、またはコマンドが非ゼロで終了した場合
     * @throws InterruptedException コマンド実行中に中断された場合
     * @throws TimeoutException     タイムアウトした場合
     */
    public String executeCommand(String commandPath, List<String> args, int timeoutSeconds)
            throws IOException, InterruptedException, TimeoutException {

        logger.debug("コマンドを実行します: {} (引数: {}, タイムアウト: {}秒)", commandPath, args, timeoutSeconds);

        // コマンドリストを構築
        List<String> command = new ArrayList<>();
        command.add(commandPath);
        command.addAll(args);

        // ProcessBuilderを使用してプロセスを起動
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false); // 標準エラーと標準出力を分離

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            logger.error("コマンドの起動に失敗しました: {}", commandPath, e);
            throw new IOException("コマンドの起動に失敗しました: " + commandPath, e);
        }

        // 標準出力を読み取る
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();

        // 標準出力の読み取りスレッド
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                logger.warn("標準出力の読み取り中にエラーが発生しました", e);
            }
        });

        // 標準エラーの読み取りスレッド
        Thread errorReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            } catch (IOException e) {
                logger.warn("標準エラー出力の読み取り中にエラーが発生しました", e);
            }
        });

        outputReader.start();
        errorReader.start();

        // プロセスの完了を待機（タイムアウト付き）
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finished) {
            // タイムアウトした場合はプロセスを強制終了
            process.destroyForcibly();
            outputReader.interrupt();
            errorReader.interrupt();
            logger.error("コマンド実行がタイムアウトしました: {} ({}秒)", commandPath, timeoutSeconds);
            throw new TimeoutException("コマンド実行がタイムアウトしました: " + commandPath);
        }

        // 出力読み取りスレッドの完了を待つ
        outputReader.join(1000);
        errorReader.join(1000);

        int exitCode = process.exitValue();

        // 標準エラーがあればログに記録
        if (errorOutput.length() > 0) {
            logger.debug("コマンドの標準エラー出力: {}", errorOutput.toString().trim());
        }

        // 終了コードをチェック
        if (exitCode != 0) {
            String errorMessage = String.format(
                    "コマンドが非ゼロで終了しました: %s (終了コード: %d, エラー出力: %s)",
                    commandPath, exitCode, errorOutput.toString().trim());
            logger.error(errorMessage);
            throw new IOException(errorMessage);
        }

        String result = output.toString();
        logger.debug("コマンド実行が成功しました: {} (出力サイズ: {} バイト)", commandPath, result.length());

        return result;
    }
}
