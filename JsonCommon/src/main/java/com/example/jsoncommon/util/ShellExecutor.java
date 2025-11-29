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
 * 外部シェルスクリプトを実行するユーティリティクラス
 */
@Component
public class ShellExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ShellExecutor.class);

    /**
     * 外部シェルスクリプトを実行し、標準出力を返します
     *
     * @param shellPath      シェルスクリプトのパス
     * @param args           シェルスクリプトに渡す引数のリスト
     * @param timeoutSeconds タイムアウト時間（秒）
     * @return シェルスクリプトの標準出力
     * @throws IOException          シェル実行に失敗した場合、またはシェルが非ゼロで終了した場合
     * @throws InterruptedException シェル実行中に中断された場合
     * @throws TimeoutException     タイムアウトした場合
     */
    public String executeShell(String shellPath, List<String> args, int timeoutSeconds)
            throws IOException, InterruptedException, TimeoutException {

        logger.debug("シェルを実行します: {} (引数: {}, タイムアウト: {}秒)", shellPath, args, timeoutSeconds);

        // コマンドリストを構築
        List<String> command = new ArrayList<>();
        command.add(shellPath);
        command.addAll(args);

        // ProcessBuilderを使用してプロセスを起動
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false); // 標準エラーと標準出力を分離

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            logger.error("シェルの起動に失敗しました: {}", shellPath, e);
            throw new IOException("シェルの起動に失敗しました: " + shellPath, e);
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
            logger.error("シェル実行がタイムアウトしました: {} ({}秒)", shellPath, timeoutSeconds);
            throw new TimeoutException("シェル実行がタイムアウトしました: " + shellPath);
        }

        // 出力読み取りスレッドの完了を待つ
        outputReader.join(1000);
        errorReader.join(1000);

        int exitCode = process.exitValue();

        // 標準エラーがあればログに記録
        if (errorOutput.length() > 0) {
            logger.debug("シェルの標準エラー出力: {}", errorOutput.toString().trim());
        }

        // 終了コードをチェック
        if (exitCode != 0) {
            String errorMessage = String.format(
                    "シェルが非ゼロで終了しました: %s (終了コード: %d, エラー出力: %s)",
                    shellPath, exitCode, errorOutput.toString().trim());
            logger.error(errorMessage);
            throw new IOException(errorMessage);
        }

        String result = output.toString();
        logger.debug("シェル実行が成功しました: {} (出力サイズ: {} バイト)", shellPath, result.length());

        return result;
    }
}
