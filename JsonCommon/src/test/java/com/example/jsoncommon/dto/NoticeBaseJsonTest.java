package com.example.jsoncommon.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NoticeBaseJsonTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testSerializationOfMetricsJson() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        ZonedDateTime timestamp = ZonedDateTime.now();
        String agentVersion = "1.0.0";
        String instanceName = "test-instance";
        Metrics metrics = new Metrics(75.5, 60.3);

        MetricsJson metricsJson = new MetricsJson(
                id, NoticeType.METRICS, timestamp, agentVersion, instanceName, metrics);

        // Act
        String json = objectMapper.writeValueAsString(metricsJson);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"Id\""));
        assertTrue(json.contains("\"NoticeType\":\"METRICS\""));
        assertTrue(json.contains("\"AgentVersion\":\"1.0.0\""));
        assertTrue(json.contains("\"InstanceName\":\"test-instance\""));
        assertTrue(json.contains("\"CpuUsage\":75.5"));
        assertTrue(json.contains("\"MemoryUsage\":60.3"));
    }

    @Test
    void testDeserializationOfMetricsJson() throws Exception {
        // Arrange
        UUID id = UUID.randomUUID();
        String json = String.format(
                "{\"Id\":\"%s\",\"NoticeType\":\"METRICS\",\"timestamp\":\"2023-11-26T10:00:00Z[UTC]\",\"AgentVersion\":\"1.0.0\",\"InstanceName\":\"test-instance\",\"Metrics\":{\"CpuUsage\":75.5,\"MemoryUsage\":60.3}}",
                id.toString());

        // Act
        MetricsJson metricsJson = objectMapper.readValue(json, MetricsJson.class);

        // Assert
        assertNotNull(metricsJson);
        assertEquals(id, metricsJson.getId());
        assertEquals(NoticeType.METRICS, metricsJson.getNoticeType());
        assertEquals("1.0.0", metricsJson.getAgentVersion());
        assertEquals("test-instance", metricsJson.getInstanceName());
        assertNotNull(metricsJson.getMetrics());
        assertEquals(75.5, metricsJson.getMetrics().getCpuUsage());
        assertEquals(60.3, metricsJson.getMetrics().getMemoryUsage());
    }

    @Test
    void testUpJsonCreation() {
        // Arrange
        UUID id = UUID.randomUUID();
        ZonedDateTime timestamp = ZonedDateTime.now();
        String agentVersion = "1.0.0";
        String instanceName = "test-instance";

        // Act
        UpJson upJson = new UpJson(id, timestamp, agentVersion, instanceName);

        // Assert
        assertNotNull(upJson);
        assertEquals(id, upJson.getId());
        assertEquals(NoticeType.UP, upJson.getNoticeType());
        assertEquals(timestamp, upJson.getTimestamp());
        assertEquals(agentVersion, upJson.getAgentVersion());
        assertEquals(instanceName, upJson.getInstanceName());
    }

    @Test
    void testDownJsonCreation() {
        // Arrange
        UUID id = UUID.randomUUID();
        ZonedDateTime timestamp = ZonedDateTime.now();

        // Act
        DownJson downJson = new DownJson(id, timestamp, "1.0.0", "test-instance");

        // Assert
        assertNotNull(downJson);
        assertEquals(NoticeType.DOWN, downJson.getNoticeType());
    }

    @Test
    void testInstallJsonCreation() {
        // Arrange
        UUID id = UUID.randomUUID();
        ZonedDateTime timestamp = ZonedDateTime.now();

        // Act
        InstallJson installJson = new InstallJson(id, timestamp, "1.0.0", "test-instance");

        // Assert
        assertNotNull(installJson);
        assertEquals(NoticeType.INSTALL, installJson.getNoticeType());
    }

    @Test
    void testUninstallJsonCreation() {
        // Arrange
        UUID id = UUID.randomUUID();
        ZonedDateTime timestamp = ZonedDateTime.now();

        // Act
        UninstallJson uninstallJson = new UninstallJson(id, timestamp, "1.0.0", "test-instance");

        // Assert
        assertNotNull(uninstallJson);
        assertEquals(NoticeType.UNINSTALL, uninstallJson.getNoticeType());
    }
}
