package com.example.jsonsender.service;

import com.example.jsoncommon.dto.ConditionLogic;
import com.example.jsoncommon.dto.ScalingMode;
import com.example.jsoncommon.dto.Threshold;
import com.example.jsoncommon.dto.ThresholdCsv;
import com.example.jsonsender.repository.ThresholdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

class ThresholdServiceTest {

    @Mock
    private ThresholdRepository thresholdRepository;

    private ThresholdService thresholdService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        thresholdService = new ThresholdService(thresholdRepository);
    }

    @Test
    void updateThreshold_shouldSaveThresholdCsv() throws IOException {
        String hostname = "test-host";
        Threshold threshold = new Threshold(
                hostname,
                ScalingMode.AUTO,
                true, // upperChangeableEnable
                90.0, // upperCpuThreshold
                5, // upperCpuDurationMin
                80.0, // upperMemThreshold
                5, // upperMemDurationMin
                ConditionLogic.OR, // upperConditionLogic
                true, // lowerChangeableEnable
                20.0, // lowerCpuThreshold
                5, // lowerCpuDurationMin
                10.0, // lowerMemThreshold
                5, // lowerMemDurationMin
                ConditionLogic.OR, // lowerConditionLogic
                true, // microChangeableEnable
                false // microForceOnStandby
        );

        thresholdService.updateThreshold(hostname, threshold);

        ArgumentCaptor<ThresholdCsv> captor = ArgumentCaptor.forClass(ThresholdCsv.class);
        verify(thresholdRepository).save(captor.capture());

        ThresholdCsv savedInfo = captor.getValue();
        assertEquals(hostname, savedInfo.getHostname());
        assertEquals(ScalingMode.AUTO, savedInfo.getScalingMode());
        assertEquals(90.0, savedInfo.getUpperCpuThreshold());
        assertEquals(20.0, savedInfo.getLowerCpuThreshold());
    }
}
