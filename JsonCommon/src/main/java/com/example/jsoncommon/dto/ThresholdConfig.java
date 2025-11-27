package com.example.jsoncommon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdConfig {
    @JsonProperty("Hostname")
    private String hostname;

    @JsonProperty("UpperCpuThreshold")
    private int upperCpuThreshold;

    @JsonProperty("UpperCpuDuration_min")
    private int upperCpuDurationMin;

    @JsonProperty("UpperMemThreshold")
    private int upperMemThreshold;

    @JsonProperty("UpperMemDurationMin")
    private int upperMemDurationMin;

    @JsonProperty("UpperConditionLogic")
    private String upperConditionLogic;

    @JsonProperty("LowerCpuThreshold")
    private int lowerCpuThreshold;

    @JsonProperty("LowerCpuDuration_min")
    private int lowerCpuDurationMin;

    @JsonProperty("LowerMemThreshold")
    private int lowerMemThreshold;

    @JsonProperty("LowerMemDuration_min")
    private int lowerMemDurationMin;

    @JsonProperty("LpperConditionLogic")
    private String lowerConditionLogic;
}
