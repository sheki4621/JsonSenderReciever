package com.example.jsonsender;

import com.example.jsoncommon.dto.*;
import com.example.jsoncommon.tcp.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JsonSenderで受信したメッセージを処理するハンドラー
 */
@Component
@Slf4j
public class JsonSenderMessageHandler implements MessageHandler {

    @Override
    public void handleMessage(NoticeBaseJson message) {
        log.info("JsonReceiverからメッセージを受信しました: type={}, id={}, instance={}",
                message.getNoticeType(), message.getId(), message.getInstanceName());

        // 現時点では受信ログのみ。将来的に必要に応じて処理を追加
        switch (message.getNoticeType()) {
            case METRICS:
                handleMetrics((MetricsJson) message);
                break;
            case INSTALL:
                handleInstall((InstallJson) message);
                break;
            case UNINSTALL:
                handleUninstall((UninstallJson) message);
                break;
            case UP:
                handleUp((UpJson) message);
                break;
            case DOWN:
                handleDown((DownJson) message);
                break;
            case THRESHOLD:
                handleThreshold((ThresholdJson) message);
                break;
            default:
                log.warn("未知のメッセージタイプ: {}", message.getNoticeType());
        }
    }

    private void handleMetrics(MetricsJson message) {
        log.info("メトリクスデータを受信: CPU={}%, Memory={}%",
                message.getMetrics().getCpuUsage(),
                message.getMetrics().getMemoryUsage());
    }

    private void handleInstall(InstallJson message) {
        log.info("インストール通知を受信: instance={}", message.getInstanceName());
    }

    private void handleUninstall(UninstallJson message) {
        log.info("アンインストール通知を受信: instance={}", message.getInstanceName());
    }

    private void handleUp(UpJson message) {
        log.info("UP通知を受信: instance={}", message.getInstanceName());
    }

    private void handleDown(DownJson message) {
        log.info("DOWN通知を受信: instance={}", message.getInstanceName());
    }

    private void handleThreshold(ThresholdJson message) {
        log.info("しきい値変更通知を受信: instance={}, config={}",
                message.getInstanceName(), message.getThreshold());
    }
}
