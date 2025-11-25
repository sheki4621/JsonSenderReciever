package com.example.jsonsender.utils.notice;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class InstallJsonTest {

    @Test
    public void testInstallJsonCreation() {
        UUID id = UUID.randomUUID();
        ZonedDateTime timestamp = ZonedDateTime.now();
        String agentVersion = "1.0.0";
        String instanceName = "test-instance";

        InstallJson installJson = new InstallJson(id, timestamp, agentVersion, instanceName);

        assertNotNull(installJson);
        assertEquals(id, installJson.getId());
        assertEquals(NoticeType.INSTALL, installJson.getNoticeType());
        assertEquals(timestamp, installJson.getTimestamp());
        assertEquals(agentVersion, installJson.getAgentVersion());
        assertEquals(instanceName, installJson.getInstanceName());
    }

    @Test
    public void testInstallJsonNoArgsConstructor() {
        InstallJson installJson = new InstallJson();
        assertNotNull(installJson);
    }
}
