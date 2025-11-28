package com.example.jsonsender.tcp;

import com.example.jsoncommon.dto.*;
import com.example.jsoncommon.tcp.MessageHandler;
import com.example.jsonsender.service.ThresholdService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

/**
 * JsonSenderで受信したメッセージを処理するハンドラー
 */
@Component
@Slf4j
public class JsonSenderMessageHandler implements MessageHandler {

    private final ThresholdService thresholdService;

    public JsonSenderMessageHandler(ThresholdService thresholdService) {
        this.thresholdService = thresholdService;
    }

    @Override
    public void handleMessage(NoticeBaseJson message) {
        log.info("JsonReceiverからメッセージを受信しました: type={}, id={}, instance={}",
                message.getNoticeType(), message.getId(), message.getInstanceName());

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
        thresholdService.updateThreshold(message.getInstanceName(), message.getThreshold());
    }
}
