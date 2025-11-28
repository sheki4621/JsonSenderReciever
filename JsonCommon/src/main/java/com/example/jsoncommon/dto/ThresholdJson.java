package com.example.jsoncommon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ThresholdJson extends NoticeBaseJson {
    @JsonProperty("Threshold")
    private Threshold threshold;

    public ThresholdJson(Threshold threshold) {
        this.threshold = threshold;
        this.setNoticeType(NoticeType.THRESHOLD);
    }
}
