package com.example.jsonsender.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class AppConfig {

    @Value("${app.Name}")
    private String appName;

    @Value("${app.NoticeIntervalSec}")
    private int noticeIntervalSec;

    @Value("${app.Timezone}")
    private String timezone;

    @Value("${app.AgentVersion}")
    private String agentVersion;

    @Value("${app.LogLevel}")
    private String logLevel;

    @Value("${dist.Hostname}")
    private String distHostname;

    @Value("${dist.Port}")
    private int distPort;

    @Value("${sender.RetryMax}")
    private int retryMax;

    @Value("${sender.RetryIntervalSec}")
    private int retryIntervalSec;

    @Value("${sender.Timeout}")
    private int timeout;

    @Value("${json.OutputDir}")
    private String jsonOutputDir;

    @Value("${json.LotationDay}")
    private int jsonRotationDay;
}
