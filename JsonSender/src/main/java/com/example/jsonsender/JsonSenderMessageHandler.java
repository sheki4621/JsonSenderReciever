package com.example.jsonsender;

import com.example.jsoncommon.dto.*;
import com.example.jsoncommon.tcp.MessageHandler;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JsonSenderで受信したメッセージを処理するハンドラー
 */
@Component
@Slf4j
public class JsonSenderMessageHandler implements MessageHandler {

    // @Autowired
    // private ThresholdService threasholdService;

    @Override
    public void handleMessage(NoticeBaseJson message) {
        log.info("JsonReceiverからメッセージを受信しました: type={}, id={}, instance={}",
                message.getNoticeType(), message.getId(), message.getInstanceName());

        // 現時点では受信ログのみ。将来的に必要に応じて処理を追加
        switch (message.getNoticeType()) {
            case THRESHOLD:
                handleThreshold((ThresholdJson) message);
                break;
            default:
                log.warn("未知のメッセージタイプ: {}", message.getNoticeType());
        }
    }

    private void handleThreshold(ThresholdJson message) {
        log.info("しきい値変更通知を受信: instance={}, config={}",
                message.getInstanceName(), message.getThreshold());
        // threasholdService.updateThreshold(message.getInstanceName(),
        // message.getThreshold());
    }
}
