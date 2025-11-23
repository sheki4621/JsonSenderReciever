package com.example.jsonsender.metrics;

import com.example.jsonsender.utils.notice.NoticeType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.UUID;

public class MetricsJson extends com.example.jsonsender.utils.notice.NoticeBaseJson {
    @JsonProperty("Metrics")
    private Metrics metrics;

    public MetricsJson() {
        super();
    }

    public MetricsJson(UUID id, NoticeType noticeType, ZonedDateTime timestamp, String version, Metrics metrics) {
        super(id, noticeType, timestamp, version);
        this.metrics = metrics;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }
}
