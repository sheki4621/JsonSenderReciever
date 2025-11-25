package com.example.jsonsender.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AppConfig.class)
@TestPropertySource(properties = {
        "app.Name=TestApp",
        "app.NoticeIntervalSec=5",
        "app.Timezone=UTC",
        "app.AgentVersion=2.0",
        "app.LogLevel=DEBUG",
        "dist.Hostname=127.0.0.1",
        "dist.Port=8080",
        "sender.RetryMax=5",
        "sender.RetryIntervalSec=2",
        "sender.Timeout=10"
})
class AppConfigTest {

    @Autowired
    private AppConfig appConfig;

    @Test
    void testPropertiesInjection() {
        assertEquals("TestApp", appConfig.getAppName());
        assertEquals(5, appConfig.getNoticeIntervalSec());
        assertEquals("UTC", appConfig.getTimezone());
        assertEquals("2.0", appConfig.getAgentVersion());
        assertEquals("DEBUG", appConfig.getLogLevel());
        assertEquals("127.0.0.1", appConfig.getDistHostname());
        assertEquals(8080, appConfig.getDistPort());
        assertEquals(5, appConfig.getRetryMax());
        assertEquals(2, appConfig.getRetryIntervalSec());
        assertEquals(10, appConfig.getTimeout());
    }
}
