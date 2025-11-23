package com.example.jsonsender.utils.notice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class NoticeBaseJson {
    @JsonProperty("Id")
    private UUID id;

    @JsonProperty("NoticeType")
    private NoticeType noticeType;

    @JsonProperty("timestamp")
    private ZonedDateTime timestamp;

    @JsonProperty("AgentVersion")
    private String agentVersion;
}
