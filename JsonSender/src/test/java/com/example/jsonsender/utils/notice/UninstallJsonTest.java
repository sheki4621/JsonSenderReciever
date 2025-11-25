package com.example.jsonsender.utils.notice;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UninstallJsonTest {

    @Test
    public void testUninstallJsonCreation() {
        UUID id = UUID.randomUUID();
        ZonedDateTime timestamp = ZonedDateTime.now();
        String agentVersion = "1.0.0";
        String instanceName = "test-instance";

        UninstallJson uninstallJson = new UninstallJson(id, timestamp, agentVersion, instanceName);

        assertNotNull(uninstallJson);
        assertEquals(id, uninstallJson.getId());
        assertEquals(NoticeType.UNINSTALL, uninstallJson.getNoticeType());
        assertEquals(timestamp, uninstallJson.getTimestamp());
        assertEquals(agentVersion, uninstallJson.getAgentVersion());
        assertEquals(instanceName, uninstallJson.getInstanceName());
    }

    @Test
    public void testUninstallJsonNoArgsConstructor() {
        UninstallJson uninstallJson = new UninstallJson();
        assertNotNull(uninstallJson);
    }
}
