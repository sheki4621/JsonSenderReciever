package com.example.jsonsender.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AppConfig.class)
@TestPropertySource(properties = {
        "app.name=TestApp",
        "app.notice-interval-sec=5",
        "app.timezone=UTC",
        "app.agent-version=2.0",
        "app.log-level=DEBUG",
        "app.error-retry-interval-sec=3",
        "app.dist.hostname=127.0.0.1",
        "app.dist.port=8080",
        "app.sender.retry-max=5",
        "app.sender.retry-interval-sec=2",
        "app.sender.timeout=10"
})
class AppConfigTest {

    @Autowired
    private AppConfig appConfig;

    @Test
    void testPropertiesInjection() {
        assertEquals("TestApp", appConfig.getName());
        assertEquals(5, appConfig.getNoticeIntervalSec());
        assertEquals("UTC", appConfig.getTimezone());
        assertEquals("2.0", appConfig.getAgentVersion());
        assertEquals("DEBUG", appConfig.getLogLevel());
        assertEquals(3, appConfig.getErrorRetryIntervalSec());
        assertEquals("127.0.0.1", appConfig.getDist().getHostname());
        assertEquals(8080, appConfig.getDist().getPort());
        assertEquals(5, appConfig.getSender().getRetryMax());
        assertEquals(2, appConfig.getSender().getRetryIntervalSec());
        assertEquals(10, appConfig.getSender().getTimeout());
    }
}
