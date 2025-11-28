package com.example.jsonsender;

import com.example.jsoncommon.dto.NoticeType;
import com.example.jsoncommon.dto.ThresholdConfig;
import com.example.jsoncommon.dto.ThresholdJson;
import com.example.jsonsender.service.ThresholdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith({ MockitoExtension.class, OutputCaptureExtension.class })
class JsonSenderMessageHandlerTest {

    @Mock
    private ThresholdService thresholdService;

    private JsonSenderMessageHandler messageHandler;

    @BeforeEach
    void setUp() {
        messageHandler = new JsonSenderMessageHandler(thresholdService);
    }

    @Test
    void handleMessage_shouldLogThresholdUpdateAndCallService(CapturedOutput output) {
        ThresholdConfig config = new ThresholdConfig();
        config.setHostname("test-host");
        config.setUpperCpuThreshold(90.0);

        ThresholdJson message = new ThresholdJson(config);
        message.setInstanceName("test-instance");
        message.setNoticeType(NoticeType.THRESHOLD);

        messageHandler.handleMessage(message);

        assertThat(output.getOut()).contains(
                "しきい値変更通知を受信: instance=test-instance, config=ThresholdConfig(hostname=test-host, scalingMode=null, upperChangeableEnable=null, upperCpuThreshold=90.0, upperCpuDurationMin=0, upperMemThreshold=null, upperMemDurationMin=0, upperConditionLogic=null, lowerChangeableEnable=null, lowerCpuThreshold=null, lowerCpuDurationMin=0, lowerMemThreshold=null, lowerMemDurationMin=0, lowerConditionLogic=null, microChangeableEnable=null, microForceOnStandby=null)");

        verify(thresholdService).updateThreshold("test-instance", config);
    }
}
