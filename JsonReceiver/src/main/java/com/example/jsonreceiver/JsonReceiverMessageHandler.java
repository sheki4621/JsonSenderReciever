package com.example.jsonreceiver;

import com.example.jsoncommon.dto.*;
import com.example.jsoncommon.tcp.MessageHandler;
import com.example.jsonreceiver.service.InstanceStatusService;
import com.example.jsonreceiver.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JsonReceiverで受信したメッセージを処理するハンドラー
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JsonReceiverMessageHandler implements MessageHandler {

    private final MetricsService metricsService;
    private final InstanceStatusService instanceStatusService;

    @Override
    public void handleMessage(NoticeBaseJson message) {
        log.info("メッセージを受信しました: type={}, id={}, instance={}",
                message.getNoticeType(), message.getId(), message.getInstanceName());

        try {
            switch (message.getNoticeType()) {
                case METRICS:
                    handleMetrics((MetricsJson) message);
                    break;

                case UP:
                    handleUp((UpJson) message);
                    break;
                case DOWN:
                    handleDown((DownJson) message);
                    break;
                default:
                    log.warn("未知のメッセージタイプ: {}", message.getNoticeType());
            }
        } catch (Exception e) {
            log.error("メッセージ処理中にエラーが発生しました: type={}", message.getNoticeType(), e);
        }
    }

    private void handleMetrics(MetricsJson message) {
        log.info("メトリクスデータを処理: CPU={}%, Memory={}%",
                message.getMetrics().getCpuUsage(),
                message.getMetrics().getMemoryUsage());
        metricsService.processMetrics(message);
    }

    private void handleUp(UpJson message) throws IOException {
        log.info("UP通知を処理: instance={}", message.getInstanceName());
        instanceStatusService.processUp(message);
    }

    private void handleDown(DownJson message) throws IOException {
        log.info("DOWN通知を処理: instance={}", message.getInstanceName());
        instanceStatusService.processDown(message);
    }
}
