package com.example.jsoncommon.dto;

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
public class UninstallJson extends NoticeBaseJson {

    public UninstallJson(UUID id, ZonedDateTime timestamp, String agentVersion, String instanceName) {
        super(id, NoticeType.UNINSTALL, timestamp, agentVersion, instanceName);
    }
}
