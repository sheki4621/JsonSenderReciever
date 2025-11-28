package com.example.jsonreceiver.service;

import com.example.jsoncommon.dto.Threshold;
import com.example.jsoncommon.dto.ThresholdJson;
import com.example.jsoncommon.tcp.TcpClient;
import com.example.jsoncommon.tcp.TcpConfig;
import com.example.jsonreceiver.dto.InstanceStatusCsv;
import com.example.jsonreceiver.repository.InstanceStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThresholdChangeServiceTest {

    @Mock
    private TcpClient tcpClient;

    @Mock
    private InstanceStatusRepository instanceStatusRepository;

    private ThresholdChangeService thresholdChangeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        thresholdChangeService = new ThresholdChangeService(tcpClient, instanceStatusRepository);
        ReflectionTestUtils.setField(thresholdChangeService, "targetPort", 8888);
    }

    @Test
    void sendThresholdUpdate_shouldSendCorrectMessage() throws Exception {
        String instanceName = "test-instance";
        Threshold config = new Threshold();
        config.setHostname("test-host");
        config.setUpperCpuThreshold(80.0);

        InstanceStatusCsv mockStatus = new InstanceStatusCsv();
        mockStatus.setAgentVersion("2.0");
        when(instanceStatusRepository.findByHostname(instanceName)).thenReturn(Optional.of(mockStatus));

        thresholdChangeService.sendThresholdUpdate(instanceName, config);

        ArgumentCaptor<ThresholdJson> captor = ArgumentCaptor.forClass(ThresholdJson.class);
        verify(tcpClient).sendJson(eq("localhost"), eq(8888), captor.capture(),
                org.mockito.ArgumentMatchers.any(TcpConfig.class));

        ThresholdJson sentMessage = captor.getValue();
        assertEquals(instanceName, sentMessage.getInstanceName());
        assertEquals(config, sentMessage.getThreshold());
        assertEquals("2.0", sentMessage.getAgentVersion());
    }

    @Test
    void sendThresholdUpdate_shouldNotSend_whenNotFound() throws Exception {
        String instanceName = "unknown-instance";
        Threshold config = new Threshold();
        config.setHostname("test-host");

        when(instanceStatusRepository.findByHostname(instanceName)).thenReturn(Optional.empty());

        thresholdChangeService.sendThresholdUpdate(instanceName, config);

        verify(tcpClient, org.mockito.Mockito.never()).sendJson(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(ThresholdJson.class),
                org.mockito.ArgumentMatchers.any(TcpConfig.class));
    }
}
