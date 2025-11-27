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
    private ThresholdConfig threshold;

    public ThresholdJson(ThresholdConfig threshold) {
        this.threshold = threshold;
        this.setNoticeType(NoticeType.THRESHOLD);
    }
}
