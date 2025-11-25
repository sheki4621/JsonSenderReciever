package com.example.jsonreceiver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DownJson extends NoticeBaseJson {

    public DownJson(UUID id, NoticeType noticeType, ZonedDateTime timestamp, String agentVersion, String instanceName) {
        super(id, noticeType, timestamp, agentVersion, instanceName);
    }
}
