package com.example.jsonsender;

import com.example.jsonsender.tcp.TcpClient;

import com.example.jsonsender.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class JsonFileManager {

    private static final Logger logger = LoggerFactory.getLogger(JsonFileManager.class);
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private final TcpClient tcpClient;
    private final ExecutorService resendExecutor;
    private volatile boolean running = true;

    public JsonFileManager(AppConfig appConfig, @Lazy TcpClient tcpClient, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.tcpClient = tcpClient;
        this.objectMapper = objectMapper;
        this.resendExecutor = Executors.newSingleThreadExecutor();
    }

    private final java.util.concurrent.atomic.AtomicBoolean isResending = new java.util.concurrent.atomic.AtomicBoolean(
            false);

    @PostConstruct
    public void init() {
        createOutputDir();
    }

    @PreDestroy
    public void cleanup() {
        running = false;
        resendExecutor.shutdown();
        try {
            if (!resendExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                resendExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            resendExecutor.shutdownNow();
        }
    }

    private void createOutputDir() {
        File dir = new File(appConfig.getJson().getOutputDir());
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                logger.info("JSON出力ディレクトリを作成しました: {}", dir.getAbsolutePath());
            } else {
                logger.error("JSON出力ディレクトリの作成に失敗しました: {}", dir.getAbsolutePath());
            }
        }
    }

    public void save(Object data) {
        try {
            createOutputDir();
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
                    .withZone(ZoneId.of(appConfig.getTimezone()))
                    .format(Instant.now());

            com.fasterxml.jackson.databind.JsonNode node = objectMapper.valueToTree(data);
            String noticeType = node.has("NoticeType") ? node.get("NoticeType").asText() : "Unknown";

            String filename = String.format("%s_%s.json", timestamp, noticeType);
            Path path = Paths.get(appConfig.getJson().getOutputDir(), filename);

            objectMapper.writeValue(path.toFile(), node);
            logger.warn("送信失敗したJSONをファイルに保存しました: {}", path);
        } catch (IOException e) {
            logger.error("JSONファイルの保存に失敗しました", e);
        } catch (Exception e) {
            logger.error("JSONファイルの保存に失敗しました", e);
        }
    }

    public void resendAsync() {
        if (isResending.compareAndSet(false, true)) {
            resendExecutor.submit(() -> {
                try {
                    processFiles();
                } catch (Exception e) {
                    logger.error("再送信タスクでエラーが発生しました", e);
                } finally {
                    isResending.set(false);
                }
            });
        }
    }

    private void processFiles() {
        File dir = new File(appConfig.getJson().getOutputDir());
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return;
        }

        logger.info("再送信する失敗JSONファイルを{}件見つけました", files.length);

        // Sort by modification time (oldest first)
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));

        for (File file : files) {
            if (!running)
                break;

            // Check for rotation (deletion of old files)
            if (isOldFile(file)) {
                if (file.delete()) {
                    logger.info("古いファイルを削除しました: {}", file.getName());
                } else {
                    logger.warn("古いファイルの削除に失敗しました: {}", file.getName());
                }
                continue;
            }

            // Try to resend
            try {
                // Read JSON to check validity/type if needed, but here we just read as Object
                // or specific type if known.
                // Since we don't know the exact type easily without metadata, we might need to
                // read as Map or JsonNode.
                // However, TcpClient.sendJson takes Object.
                // Let's read as JsonNode to be generic.
                com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(file);

                // We need to send it. We should use a method in TcpClient that DOES NOT save on
                // failure,
                // to avoid infinite loops of saving the same file.
                // But TcpClient currently only has one method. We will modify TcpClient later.
                // For now, assume we will add a boolean flag to sendJson.

                boolean success = tcpClient.sendJsonDirectly(appConfig.getDist().getHostname(),
                        appConfig.getDist().getPort(), jsonNode);

                if (success) {
                    if (file.delete()) {
                        logger.info("再送信してファイルを削除しました: {}", file.getName());
                    } else {
                        logger.warn("再送信しましたがファイルの削除に失敗しました: {}", file.getName());
                    }
                } else {
                    break;
                }

            } catch (IOException e) {
                logger.error("ファイルの読み込みまたは解析に失敗しました: {}", file.getName(), e);
            }
        }
    }

    private boolean isOldFile(File file) {
        long diff = System.currentTimeMillis() - file.lastModified();
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        return days >= appConfig.getJson().getRotationDay();
    }
}
