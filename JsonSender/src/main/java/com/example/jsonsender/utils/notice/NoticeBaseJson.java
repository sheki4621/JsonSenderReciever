package com.example.jsonsender.utils.notice;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.UUID;

public abstract class NoticeBaseJson {
    @JsonProperty("Id")
    private UUID id;

    @JsonProperty("NoticeType")
    private NoticeType noticeType;

    @JsonProperty("timestamp")
    private ZonedDateTime timestamp;

    @JsonProperty("Version")
    private String version;

    public NoticeBaseJson() {
    }

    public NoticeBaseJson(UUID id, NoticeType noticeType, ZonedDateTime timestamp, String version) {
        this.id = id;
        this.noticeType = noticeType;
        this.timestamp = timestamp;
        this.version = version;
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
}
