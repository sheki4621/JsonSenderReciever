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
public class InstallJson extends NoticeBaseJson {

    public InstallJson(UUID id, ZonedDateTime timestamp, String agentVersion, String instanceName) {
        super(id, NoticeType.INSTALL, timestamp, agentVersion, instanceName);
    }
}
