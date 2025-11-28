package com.example.jsonreceiver.monitortarget;

import com.example.jsoncommon.dto.UpJson;
import com.example.jsoncommon.dto.DownJson;
import com.example.jsonreceiver.instancetype.*;
import com.example.jsonreceiver.util.ShellExecutor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.io.IOException;
import java.util.Optional;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * InstanceStatus処理サービス
 * INSTALL, UP, DOWN, UNINSTALL通知に応じてインスタンスステータスを管理します。
 */
@Service
@RequiredArgsConstructor
public class InstanceStatusService {

    private static final Logger logger = LoggerFactory.getLogger(InstanceStatusService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final InstanceStatusRepository repository;
    private final AllInstanceRepository allInstanceRepository;
    private final InstanceTypeLinkRepository instanceTypeLinkRepository;
    private final InstanceTypeRepository instanceTypeRepository;
    private final ShellExecutor shellExecutor;

    @Value("${shell.agent.install.path:/path/to/install_agent.sh}")
    private String installAgentShellPath;

    @Value("${shell.agent.uninstall.path:/path/to/uninstall_agent.sh}")
    private String uninstallAgentShellPath;

    @Value("${shell.execution.timeout-seconds:30}")
    private int shellTimeoutSeconds;

    /**
     * UP通知を処理します
     * ステータスをUPに変更します
     * 
     * @param upJson UP通知JSON
     */
    public void processUp(UpJson upJson) {
        try {
            logger.info("インスタンス {} の UP 通知を処理しています", upJson.getInstanceName());

            // InstanceStatusを構築して保存
            InstanceStatusCsv status = buildInstanceStatus(
                    upJson.getInstanceName(),
                    InstanceStatusValue.UP,
                    upJson.getAgentVersion(),
                    upJson.getTimestamp().toString());

            repository.save(status);
            logger.info("インスタンス {} の UP ステータスを保存しました", upJson.getInstanceName());

            // AGENT_LAST_NOTICE_TIMEを更新
            String currentTime = ZonedDateTime.now().format(TIMESTAMP_FORMATTER);
            repository.updateAgentLastNoticeTime(upJson.getInstanceName(), currentTime);
            logger.info("ホスト名 {} のAGENT_LAST_NOTICE_TIMEを更新しました: {}",
                    upJson.getInstanceName(), currentTime);
        } catch (IOException e) {
            logger.error("UP 通知の処理に失敗しました", e);
            throw new RuntimeException("Failed to process UP notification", e);
        }
    }

    /**
     * DOWN通知を処理します
     * ステータスをDOWNに変更します
     * 
     * @param downJson DOWN通知JSON
     */
    public void processDown(DownJson downJson) {
        try {
            logger.info("インスタンス {} の DOWN 通知を処理しています", downJson.getInstanceName());

            // InstanceStatusを構築して保存
            InstanceStatusCsv status = buildInstanceStatus(
                    downJson.getInstanceName(),
                    InstanceStatusValue.DOWN,
                    downJson.getAgentVersion(),
                    downJson.getTimestamp().toString());

            repository.save(status);
            logger.info("インスタンス {} の DOWN ステータスを保存しました", downJson.getInstanceName());

            // AGENT_LAST_NOTICE_TIMEを更新
            String currentTime = ZonedDateTime.now().format(TIMESTAMP_FORMATTER);
            repository.updateAgentLastNoticeTime(downJson.getInstanceName(), currentTime);
            logger.info("ホスト名 {} のAGENT_LAST_NOTICE_TIMEを更新しました: {}",
                    downJson.getInstanceName(), currentTime);
        } catch (IOException e) {
            logger.error("DOWN 通知の処理に失敗しました", e);
            throw new RuntimeException("Failed to process DOWN notification", e);
        }
    }

    /**
     * InstanceStatusオブジェクトを構築します
     * SystemInfo.csv、InstanceTypeLinkCsv.csv、InstanceType.csvから必要な情報を取得します
     * 
     * @param hostname     ホスト名
     * @param agentStatus  エージェント状態
     * @param agentVersion エージェントバージョン
     * @param lastUpdate   最終更新時刻
     * @return InstanceStatusCsv
     * @throws IOException IO例外
     */
    private InstanceStatusCsv buildInstanceStatus(String hostname, InstanceStatusValue agentStatus,
            String agentVersion, String lastUpdate) throws IOException {
        String machineType = "";
        String region = "";
        String currentType = "";
        String typeId = "";
        String typeHigh = "";
        String typeSmallStandard = "";
        String typeMicro = "";

        // 既存データから現在値を取得
        Optional<InstanceStatusCsv> existingOpt = repository.findByHostname(hostname);
        if (existingOpt.isPresent()) {
            InstanceStatusCsv existing = existingOpt.get();
            machineType = existing.getMachineType();
            region = existing.getRegion();
            currentType = existing.getCurrentType();
            typeId = existing.getTypeId();
            typeHigh = existing.getTypeHigh();
            typeSmallStandard = existing.getTypeSmallStandard();
            typeMicro = existing.getTypeMicro();
        }

        // all_instance.csvからMACHINE_TYPEを取得
        try {
            Optional<AllInstanceCsv> allInstanceOpt = allInstanceRepository.findByHostname(hostname);
            if (allInstanceOpt.isPresent()) {
                machineType = allInstanceOpt.get().getMachineType();
                logger.debug("ホスト {} の MACHINE_TYPE を取得: {}", hostname, machineType);

                // InstanceTypeLinkCsv.csvからInstanceTypeId（TYPE_ID）を取得
                Optional<InstanceTypeLinkCsv> linkOpt = instanceTypeLinkRepository.findByElType(machineType);
                if (linkOpt.isPresent()) {
                    typeId = linkOpt.get().getInstanceTypeId();
                    logger.debug("MACHINE_TYPE {} の TYPE_ID を取得: {}", machineType, typeId);

                    // InstanceType.csvから各インスタンスタイプを取得
                    Optional<InstanceTypeInfoCsv> typeInfoOpt = instanceTypeRepository.findByInstanceTypeId(typeId);
                    if (typeInfoOpt.isPresent()) {
                        InstanceTypeInfoCsv typeInfo = typeInfoOpt.get();
                        typeHigh = typeInfo.getHighInstanceType();
                        typeSmallStandard = typeInfo.getLowInstanceType();
                        typeMicro = typeInfo.getVeryLowInstanceType();
                        logger.debug("TYPE_ID {} のインスタンスタイプを取得: HIGH={}, STANDARD={}, MICRO={}",
                                typeId, typeHigh, typeSmallStandard, typeMicro);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("ホスト {} のシステム情報の取得に失敗しました。既存値または空文字列を使用します", hostname, e);
        }

        // REGIONは現在のところ取得元がないため、既存値または空文字列のまま

        return new InstanceStatusCsv(
                hostname,
                machineType,
                region,
                currentType,
                typeId,
                typeHigh,
                typeSmallStandard,
                typeMicro,
                lastUpdate,
                agentStatus,
                agentVersion,
                "" // agentLastNoticeTimeは空文字列（別途更新）
        );
    }

    /**
     * Agentをインストールする
     * 外部シェルスクリプトを実行してインストール処理を行います
     * 
     * @param instanceName インスタンス名
     */
    public void installAgent(String instanceName) {
        logger.info("インスタンス {} にエージェントをインストールしています", instanceName);

        try {
            // 外部シェルを実行 (引数:インスタンス名)
            String output = shellExecutor.executeShell(
                    installAgentShellPath,
                    List.of(instanceName),
                    shellTimeoutSeconds);

            logger.info("インスタンス {} のエージェントインストールが完了しました: {}", instanceName,
                    output.trim());

        } catch (Exception e) {
            logger.error("インスタンス {} のエージェントインストールに失敗しました", instanceName, e);
            // エラーをスローせず、ログ出力のみにとどめる（要件によるが、ここでは処理を継続させるため）
            // throw new RuntimeException("Agent インストールに失敗しました: " + instanceName, e);
        }
    }

    /**
     * Agentをアンインストールする
     * 外部シェルスクリプトを実行してアンインストール処理を行います
     * 
     * @param instanceName インスタンス名
     */
    public void uninstallAgent(String instanceName) {
        logger.info("インスタンス {} からエージェントをアンインストールしています", instanceName);

        try {
            // 外部シェルを実行 (引数:インスタンス名)
            String output = shellExecutor.executeShell(
                    uninstallAgentShellPath,
                    List.of(instanceName),
                    shellTimeoutSeconds);

            logger.info("インスタンス {} のエージェントアンインストールが完了しました: {}", instanceName,
                    output.trim());

        } catch (Exception e) {
            logger.error("インスタンス {} のエージェントアンインストールに失敗しました", instanceName, e);
            // エラーをスローせず、ログ出力のみにとどめる
            // throw new RuntimeException("Agent アンインストールに失敗しました: " + instanceName, e);
        }
    }
}
