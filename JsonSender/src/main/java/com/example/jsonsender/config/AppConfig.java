package com.example.jsonsender.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
@Validated
public class AppConfig {

    @NotBlank
    private String name;

    @Min(1)
    private int noticeIntervalSec;

    @NotBlank
    private String timezone;

    @NotBlank
    private String agentVersion;

    @NotBlank
    private String logLevel;

    @Min(1)
    private int errorRetryIntervalSec;

    private Dist dist = new Dist();
    private Sender sender = new Sender();
    private Json json = new Json();

    @Getter
    @Setter
    public static class Dist {
        @NotBlank
        private String hostname;

        @Min(1)
        private int port;
    }

    @Getter
    @Setter
    public static class Sender {
        @Min(0)
        private int retryMax;

        @Min(1)
        private int retryIntervalSec;

        @Min(1)
        private int timeout;
    }

    @Getter
    @Setter
    public static class Json {
        @NotBlank
        private String outputDir;

        @Min(1)
        private int rotationDay;

        private boolean failedArchive;
    }
}
