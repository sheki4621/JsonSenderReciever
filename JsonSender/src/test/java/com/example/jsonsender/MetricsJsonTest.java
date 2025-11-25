package com.example.jsonsender;

import com.example.jsonsender.metrics.Metrics;
import com.example.jsonsender.metrics.MetricsJson;
import com.example.jsonsender.utils.notice.NoticeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MetricsJsonTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void testMetricsJsonSerialization() throws Exception {
        UUID id = com.example.jsonsender.utils.IdUtils.getId();
        ZonedDateTime now = com.example.jsonsender.utils.TimeUtils.getNow("Asia/Tokyo");
        Metrics metrics = new Metrics(23.4, 34.5);
        MetricsJson metricsJson = new MetricsJson(id, NoticeType.METRICS, now, "1.0", "test-instance", metrics);

        String json = objectMapper.writeValueAsString(metricsJson);
        System.out.println(json);

        assertTrue(json.contains("\"Id\":"));
        assertTrue(json.contains("\"NoticeType\":\"METRICS\""));
        assertTrue(json.contains("\"timestamp\":"));
        assertTrue(json.contains("\"AgentVersion\":\"1.0\""));
        assertTrue(json.contains("\"InstanceName\":\"test-instance\""));
        assertTrue(json.contains("\"Metrics\":"));
        assertTrue(json.contains("\"CpuUsage\":23.4"));
        assertTrue(json.contains("\"MemoryUsage\":34.5"));
    }

    @Test
    void testMetricsJsonWithNullValues() throws Exception {
        UUID id = com.example.jsonsender.utils.IdUtils.getId();
        ZonedDateTime now = com.example.jsonsender.utils.TimeUtils.getNow("Asia/Tokyo");
        Metrics metrics = new Metrics(null, null);
        MetricsJson metricsJson = new MetricsJson(id, NoticeType.METRICS, now, "1.0", "test-instance", metrics);

        String json = objectMapper.writeValueAsString(metricsJson);

        assertTrue(json.contains("\"CpuUsage\":null"));
        assertTrue(json.contains("\"MemoryUsage\":null"));
    }
}
