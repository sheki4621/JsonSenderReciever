package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.*;
import com.example.jsonreceiver.repository.InstanceStatusRepository;
import com.example.jsonreceiver.util.ShellExecutor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
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
    private final ShellExecutor shellExecutor;

    @Value("${shell.agent.install.path:/path/to/install_agent.sh}")
    private String installAgentShellPath;

    @Value("${shell.agent.uninstall.path:/path/to/uninstall_agent.sh}")
    private String uninstallAgentShellPath;

    @Value("${shell.execution.timeout-seconds:30}")
    private int shellTimeoutSeconds;

    /**
     * INSTALL通知を処理します
     * ステータスをINSTALLINGに変更し、Agentをインストール（空実装）します
     * 
     * @param installJson INSTALL通知JSON
     */
    public void processInstall(InstallJson installJson) {
        try {
            logger.info("インスタンス {} の INSTALL 通知を処理しています", installJson.getInstanceName());

            // Agentのインストール（rshを想定しているが空実装）
            installAgent(installJson.getInstanceName());

            // 既存のInstanceTypeを取得
            InstanceType existingInstanceType = null;
            try {
                Optional<InstanceStatus> existingOpt = repository.findByHostname(installJson.getInstanceName());
                if (existingOpt.isPresent()) {
                    existingInstanceType = existingOpt.get().getInstanceType();
                }
            } catch (IOException e) {
                logger.warn("既存のインスタンスタイプの取得に失敗、null を使用します", e);
            }

            // ステータスをINSTALLINGに変更
            InstanceStatus status = new InstanceStatus(
                    installJson.getInstanceName(),
                    InstanceStatusValue.INSTALLING,
                    false,
                    installJson.getAgentVersion(),
                    installJson.getTimestamp().toString(),
                    existingInstanceType);

            repository.save(status);
            logger.info("インスタンス {} の INSTALLING ステータスを保存しました", installJson.getInstanceName());
        } catch (IOException e) {
            logger.error("INSTALL 通知の処理に失敗しました", e);
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
            logger.info("インスタンス {} の UNINSTALL 通知を処理しています", uninstallJson.getInstanceName());

            // Agentのアンインストール（rshを想定しているが空実装）
            uninstallAgent(uninstallJson.getInstanceName());

            // 既存のInstanceTypeを取得
            InstanceType existingInstanceType = null;
            try {
                Optional<InstanceStatus> existingOpt = repository.findByHostname(uninstallJson.getInstanceName());
                if (existingOpt.isPresent()) {
                    existingInstanceType = existingOpt.get().getInstanceType();
                }
            } catch (IOException e) {
                logger.warn("既存のインスタンスタイプの取得に失敗、null を使用します", e);
            }

            // ステータスをUNINSTALLINGに変更
            InstanceStatus status = new InstanceStatus(
                    uninstallJson.getInstanceName(),
                    InstanceStatusValue.UNINSTALLING,
                    false,
                    uninstallJson.getAgentVersion(),
                    uninstallJson.getTimestamp().toString(),
                    existingInstanceType);

            repository.save(status);
            logger.info("インスタンス {} の UNINSTALLING ステータスを保存しました", uninstallJson.getInstanceName());
        } catch (IOException e) {
            logger.error("UNINSTALL 通知の処理に失敗しました", e);
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
            logger.info("インスタンス {} の UP 通知を処理しています", upJson.getInstanceName());

            // 既存データを取得
            Optional<InstanceStatus> existingOpt = repository.findByHostname(upJson.getInstanceName());

            boolean isInstalled = true; // デフォルトはtrue
            InstanceType existingInstanceType = null;
            if (existingOpt.isPresent()) {
                InstanceStatus existing = existingOpt.get();
                // INSTALLINGからUPの場合はIsInstalledをtrueに
                if (existing.getStatus() == InstanceStatusValue.INSTALLING) {
                    isInstalled = true;
                } else {
                    // その他の場合は既存値を保持
                    isInstalled = existing.getIsInstalled();
                }
                // InstanceTypeを保持
                existingInstanceType = existing.getInstanceType();
            }

            // ステータスをUPに変更
            InstanceStatus status = new InstanceStatus(
                    upJson.getInstanceName(),
                    InstanceStatusValue.UP,
                    isInstalled,
                    upJson.getAgentVersion(),
                    upJson.getTimestamp().toString(),
                    existingInstanceType);

            repository.save(status);
            logger.info("インスタンス {} の UP ステータスを保存しました", upJson.getInstanceName());
        } catch (IOException e) {
            logger.error("UP 通知の処理に失敗しました", e);
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
            logger.info("インスタンス {} の DOWN 通知を処理しています", downJson.getInstanceName());

            // 既存データを取得
            // UPの場合、前の状態がINSTALLINGだったらisInstalledをtrueに
            InstanceType existingInstanceType = null;
            boolean isInstalled = false; // デフォルトはfalse (DOWNなので)
            try {
                Optional<InstanceStatus> existingOpt = repository.findByHostname(downJson.getInstanceName());
                if (existingOpt.isPresent()) {
                    InstanceStatus existing = existingOpt.get();
                    existingInstanceType = existing.getInstanceType();
                    // 前のステータスがUNINSTALLINGの場合はisInstalledをfalseに
                    if (existing.getStatus() == InstanceStatusValue.UNINSTALLING) {
                        isInstalled = false;
                    } else {
                        // その他の場合は既存値を保持
                        isInstalled = existing.getIsInstalled();
                    }
                }
            } catch (IOException e) {
                logger.warn("DOWN の既存ステータスの取得に失敗しました", e);
            }

            InstanceStatus status = new InstanceStatus(
                    downJson.getInstanceName(),
                    InstanceStatusValue.DOWN,
                    isInstalled,
                    downJson.getAgentVersion(),
                    downJson.getTimestamp().toString(),
                    existingInstanceType);

            repository.save(status);
            logger.info("インスタンス {} の DOWN ステータスを保存しました", downJson.getInstanceName());
        } catch (IOException e) {
            logger.error("DOWN 通知の処理に失敗しました", e);
            throw new RuntimeException("Failed to process DOWN notification", e);
        }
    }

    /**
     * Agentをインストールする
     * 外部シェルスクリプトを実行してインストール処理を行います
     * 
     * @param instanceName インスタンス名
     */
    private void installAgent(String instanceName) {
        logger.info("インスタンス {} にエージェントをインストールしています", instanceName);

        try {
            // 外部シェルを実行 (引数:インスタンス名)
            // String output = shellExecutor.executeShell(
            // installAgentShellPath,
            // List.of(instanceName),
            // shellTimeoutSeconds);

            // logger.info("インスタンス {} のエージェントインストールが完了しました: {}", instanceName,
            // output.trim());

        } catch (Exception e) {
            logger.error("インスタンス {} のエージェントインストールに失敗しました", instanceName, e);
            throw new RuntimeException("Agent インストールに失敗しました: " + instanceName, e);
        }
    }

    /**
     * Agentをアンインストールする
     * 外部シェルスクリプトを実行してアンインストール処理を行います
     * 
     * @param instanceName インスタンス名
     */
    private void uninstallAgent(String instanceName) {
        logger.info("インスタンス {} からエージェントをアンインストールしています", instanceName);

        try {
            // 外部シェルを実行 (引数:インスタンス名)
            // String output = shellExecutor.executeShell(
            // uninstallAgentShellPath,
            // List.of(instanceName),
            // shellTimeoutSeconds);

            // logger.info("インスタンス {} のエージェントアンインストールが完了しました: {}", instanceName,
            // output.trim());

        } catch (Exception e) {
            logger.error("インスタンス {} のエージェントアンインストールに失敗しました", instanceName, e);
            throw new RuntimeException("Agent アンインストールに失敗しました: " + instanceName, e);
        }
    }
}
