package com.example.jsonsender.utils.notice;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UpJsonTest {

    @Test
    public void testUpJsonCreation() {
        UUID id = UUID.randomUUID();
        ZonedDateTime timestamp = ZonedDateTime.now();
        String agentVersion = "1.0.0";
        String instanceName = "test-instance";

        UpJson upJson = new UpJson(id, timestamp, agentVersion, instanceName);

        assertNotNull(upJson);
        assertEquals(id, upJson.getId());
        assertEquals(NoticeType.UP, upJson.getNoticeType());
        assertEquals(timestamp, upJson.getTimestamp());
        assertEquals(agentVersion, upJson.getAgentVersion());
        assertEquals(instanceName, upJson.getInstanceName());
    }

    @Test
    public void testUpJsonNoArgsConstructor() {
        UpJson upJson = new UpJson();
        assertNotNull(upJson);
    }
}
