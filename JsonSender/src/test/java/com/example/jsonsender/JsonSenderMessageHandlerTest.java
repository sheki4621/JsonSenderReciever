package com.example.jsonsender;

import com.example.jsoncommon.dto.NoticeType;
import com.example.jsoncommon.dto.ThresholdConfig;
import com.example.jsoncommon.dto.ThresholdJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class JsonSenderMessageHandlerTest {

    private final JsonSenderMessageHandler messageHandler = new JsonSenderMessageHandler();

    @Test
    void handleMessage_shouldLogThresholdUpdate(CapturedOutput output) {
        ThresholdConfig config = new ThresholdConfig();
        config.setHostname("test-host");
        config.setUpperCpuThreshold(90);

        ThresholdJson message = new ThresholdJson(config);
        message.setInstanceName("test-instance");
        message.setNoticeType(NoticeType.THRESHOLD);

        messageHandler.handleMessage(message);

        assertThat(output.getOut()).contains(
                "しきい値変更通知を受信: instance=test-instance, config=ThresholdConfig(hostname=test-host, upperCpuThreshold=90, upperCpuDurationMin=0, upperMemThreshold=0, upperMemDurationMin=0, upperConditionLogic=null, lowerCpuThreshold=0, lowerCpuDurationMin=0, lowerMemThreshold=0, lowerMemDurationMin=0, lowerConditionLogic=null)");
    }
}
