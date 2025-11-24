package com.example.jsonsender.utils.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FinJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void testSerialization() throws Exception {
        UUID id = UUID.randomUUID();
        ZonedDateTime now = ZonedDateTime.now();
        FinJson notice = new FinJson(id, now, "1.0.0");

        String json = objectMapper.writeValueAsString(notice);

        assertThat(json).contains("\"Id\":\"" + id + "\"");
        assertThat(json).contains("\"NoticeType\":\"FIN\"");
        assertThat(json).contains("\"AgentVersion\":\"1.0.0\"");
    }
}
