package com.example.jsonsender.utils.notice;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DownJsonTest {

    @Test
    public void testDownJsonCreation() {
        UUID id = UUID.randomUUID();
        ZonedDateTime timestamp = ZonedDateTime.now();
        String agentVersion = "1.0.0";
        String instanceName = "test-instance";

        DownJson downJson = new DownJson(id, timestamp, agentVersion, instanceName);

        assertNotNull(downJson);
        assertEquals(id, downJson.getId());
        assertEquals(NoticeType.DOWN, downJson.getNoticeType());
        assertEquals(timestamp, downJson.getTimestamp());
        assertEquals(agentVersion, downJson.getAgentVersion());
        assertEquals(instanceName, downJson.getInstanceName());
    }

    @Test
    public void testDownJsonNoArgsConstructor() {
        DownJson downJson = new DownJson();
        assertNotNull(downJson);
    }
}
