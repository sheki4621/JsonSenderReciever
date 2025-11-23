package com.example.jsonsender.metrics;

import com.example.jsonsender.notice.NoticeType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.UUID;

public class MetricsJson {
    @JsonProperty("Id")
    private UUID id;

    @JsonProperty("NoticeType")
    private NoticeType noticeType;

    @JsonProperty("timestamp")
    private ZonedDateTime timestamp;

    @JsonProperty("Version")
    private String version;

    @JsonProperty("Metrics")
    private Metrics metrics;

    public MetricsJson() {
    }

    public MetricsJson(UUID id, NoticeType noticeType, ZonedDateTime timestamp, String version, Metrics metrics) {
        this.id = id;
        this.noticeType = noticeType;
        this.timestamp = timestamp;
        this.version = version;
        this.metrics = metrics;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public NoticeType getNoticeType() {
        return noticeType;
    }

    public void setNoticeType(NoticeType noticeType) {
        this.noticeType = noticeType;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }
}
