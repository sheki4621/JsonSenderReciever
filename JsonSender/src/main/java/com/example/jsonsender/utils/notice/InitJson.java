package com.example.jsonsender.utils.notice;

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
public class InitJson extends NoticeBaseJson {

    public InitJson(UUID id, ZonedDateTime timestamp, String agentVersion) {
        super(id, NoticeType.INIT, timestamp, agentVersion);
    }
}
