package com.example.jsonreceiver.service;

import com.example.jsoncommon.dto.ThresholdConfig;
import com.example.jsoncommon.dto.ThresholdJson;
import com.example.jsoncommon.tcp.TcpClient;
import com.example.jsoncommon.tcp.TcpConfig;
import com.example.jsonreceiver.repository.InstanceStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ThresholdChangeService {

    private final TcpClient tcpClient;
    private final InstanceStatusRepository instanceStatusRepository; // Added injection

    @Value("${tcp.server.port:8888}")
    private int serverPort;

    /**
     * しきい値変更通知を送信します。
     *
     * @param instanceName インスタンス名
     * @param config       しきい値設定
     */
    public void sendThresholdUpdate(String instanceName, ThresholdConfig config) {
        log.info("しきい値変更通知を送信します: instance={}, config={}", instanceName, config);

        String serverHost = instanceName; // TODO: IPアドレスに変換する必要ある？
        ThresholdJson message = new ThresholdJson(config);
        message.setId(UUID.randomUUID());
        message.setTimestamp(ZonedDateTime.now());
        message.setInstanceName(instanceName);

        try {
            var statusOpt = instanceStatusRepository.findByHostname(instanceName);
            if (statusOpt.isPresent()) {
                message.setAgentVersion(statusOpt.get().getAgentVersion());
            } else {
                log.error("インスタンスステータスが見つかりません: {}", instanceName);
                return;
            }
        } catch (Exception e) {
            log.error("エージェントバージョンの取得に失敗しました", e);
            return;
        }

        try {
            tcpClient.sendJson(serverHost, serverPort, message, new TcpConfig());
            log.info("しきい値変更通知を送信しました");
        } catch (Exception e) {
            log.error("しきい値変更通知の送信に失敗しました", e);
        }
    }
}
