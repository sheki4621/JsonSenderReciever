package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.*;
import com.example.jsonreceiver.repository.InstanceStatusRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

/**
 * InstanceStatus処理サービス
 * INSTALL, UP, DOWN, UNINSTALL通知に応じてインスタンスステータスを管理します。
 */
@Service
@RequiredArgsConstructor
public class InstanceStatusService {

    private static final Logger logger = LoggerFactory.getLogger(InstanceStatusService.class);

    private final InstanceStatusRepository repository;

    /**
     * INSTALL通知を処理します
     * ステータスをINSTALLINGに変更し、Agentをインストール（空実装）します
     * 
     * @param installJson INSTALL通知JSON
     */
    public void processInstall(InstallJson installJson) {
        try {
            logger.info("Processing INSTALL notification for instance: {}", installJson.getInstanceName());

            // Agentのインストール（rshを想定しているが空実装）
            installAgent(installJson.getInstanceName());

            // ステータスをINSTALLINGに変更
            InstanceStatus status = new InstanceStatus(
                    installJson.getInstanceName(),
                    InstanceStatusValue.INSTALLING,
                    false,
                    installJson.getAgentVersion(),
                    installJson.getTimestamp().toString());

            repository.save(status);
            logger.info("Saved INSTALLING status for instance: {}", installJson.getInstanceName());
        } catch (IOException e) {
            logger.error("Failed to process INSTALL notification", e);
            throw new RuntimeException("Failed to process INSTALL notification", e);
        }
    }

    /**
     * UNINSTALL通知を処理します
     * ステータスをUNINSTALLINGに変更し、Agentをアンインストール（空実装）します
     * 
     * @param uninstallJson UNINSTALL通知JSON
     */
    public void processUninstall(UninstallJson uninstallJson) {
        try {
            logger.info("Processing UNINSTALL notification for instance: {}", uninstallJson.getInstanceName());

            // Agentのアンインストール（rshを想定しているが空実装）
            uninstallAgent(uninstallJson.getInstanceName());

            // ステータスをUNINSTALLINGに変更
            InstanceStatus status = new InstanceStatus(
                    uninstallJson.getInstanceName(),
                    InstanceStatusValue.UNINSTALLING,
                    false,
                    uninstallJson.getAgentVersion(),
                    uninstallJson.getTimestamp().toString());

            repository.save(status);
            logger.info("Saved UNINSTALLING status for instance: {}", uninstallJson.getInstanceName());
        } catch (IOException e) {
            logger.error("Failed to process UNINSTALL notification", e);
            throw new RuntimeException("Failed to process UNINSTALL notification", e);
        }
    }

    /**
     * UP通知を処理します
     * ステータスをUPに変更します
     * 前のステータスがINSTALLINGの場合はIsInstalledをtrueに設定します
     * 
     * @param upJson UP通知JSON
     */
    public void processUp(UpJson upJson) {
        try {
            logger.info("Processing UP notification for instance: {}", upJson.getInstanceName());

            // 既存データを取得
            Optional<InstanceStatus> existingOpt = repository.findByHostname(upJson.getInstanceName());

            boolean isInstalled = true; // デフォルトはtrue
            if (existingOpt.isPresent()) {
                InstanceStatus existing = existingOpt.get();
                // INSTALLINGからUPの場合はIsInstalledをtrueに
                if (existing.getStatus() == InstanceStatusValue.INSTALLING) {
                    isInstalled = true;
                } else {
                    // その他の場合は既存値を保持
                    isInstalled = existing.getIsInstalled();
                }
            }

            // ステータスをUPに変更
            InstanceStatus status = new InstanceStatus(
                    upJson.getInstanceName(),
                    InstanceStatusValue.UP,
                    isInstalled,
                    upJson.getAgentVersion(),
                    upJson.getTimestamp().toString());

            repository.save(status);
            logger.info("Saved UP status for instance: {}", upJson.getInstanceName());
        } catch (IOException e) {
            logger.error("Failed to process UP notification", e);
            throw new RuntimeException("Failed to process UP notification", e);
        }
    }

    /**
     * DOWN通知を処理します
     * ステータスをDOWNに変更します
     * 前のステータスがUNINSTALLINGの場合はIsInstalledをfalseに設定します
     * 
     * @param downJson DOWN通知JSON
     */
    public void processDown(DownJson downJson) {
        try {
            logger.info("Processing DOWN notification for instance: {}", downJson.getInstanceName());

            // 既存データを取得
            Optional<InstanceStatus> existingOpt = repository.findByHostname(downJson.getInstanceName());

            boolean isInstalled = false; // デフォルトはfalse
            if (existingOpt.isPresent()) {
                InstanceStatus existing = existingOpt.get();
                // UNINSTALLINGからDOWNの場合はIsInstalledをfalseに
                if (existing.getStatus() == InstanceStatusValue.UNINSTALLING) {
                    isInstalled = false;
                } else {
                    // その他の場合は既存値を保持
                    isInstalled = existing.getIsInstalled();
                }
            }

            // ステータスをDOWNに変更
            InstanceStatus status = new InstanceStatus(
                    downJson.getInstanceName(),
                    InstanceStatusValue.DOWN,
                    isInstalled,
                    downJson.getAgentVersion(),
                    downJson.getTimestamp().toString());

            repository.save(status);
            logger.info("Saved DOWN status for instance: {}", downJson.getInstanceName());
        } catch (IOException e) {
            logger.error("Failed to process DOWN notification", e);
            throw new RuntimeException("Failed to process DOWN notification", e);
        }
    }

    /**
     * Agentをインストールする（空実装）
     * 将来的にrshを使用したインストール処理を実装する予定
     * 
     * @param instanceName インスタンス名
     */
    private void installAgent(String instanceName) {
        logger.info("Installing agent on instance: {} (stub implementation)", instanceName);
        // TODO: rshを使用したAgent インストール処理のシェルを呼び出す予定
    }

    /**
     * Agentをアンインストールする（空実装）
     * 将来的にrshを使用したアンインストール処理を実装する予定
     * 
     * @param instanceName インスタンス名
     */
    private void uninstallAgent(String instanceName) {
        logger.info("Uninstalling agent from instance: {} (stub implementation)", instanceName);
        // TODO: rshを使用したAgentアンインストール処理のシェルを呼び出す予定
    }
}
