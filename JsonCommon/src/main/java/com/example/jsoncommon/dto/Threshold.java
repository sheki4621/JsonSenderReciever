package com.example.jsoncommon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Threshold {
    @JsonProperty("Hostname")
    private String hostname;

    @JsonProperty("ScalingMode")
    private ScalingMode scalingMode;

    @JsonProperty("UpperChangeableEnable")
    private Boolean upperChangeableEnable;

    @JsonProperty("UpperCpuThreshold")
    private Double upperCpuThreshold;

    @JsonProperty("UpperCpuDuration_min")
    private int upperCpuDurationMin;

    @JsonProperty("UpperMemThreshold")
    private Double upperMemThreshold;

    @JsonProperty("UpperMemDurationMin")
    private int upperMemDurationMin;

    @JsonProperty("UpperConditionLogic")
    private ConditionLogic upperConditionLogic;

    @JsonProperty("LowerChangeableEnable")
    private Boolean lowerChangeableEnable;

    @JsonProperty("LowerCpuThreshold")
    private Double lowerCpuThreshold;

    @JsonProperty("LowerCpuDuration_min")
    private int lowerCpuDurationMin;

    @JsonProperty("LowerMemThreshold")
    private Double lowerMemThreshold;

    @JsonProperty("LowerMemDuration_min")
    private int lowerMemDurationMin;

    @JsonProperty("LowerConditionLogic")
    private ConditionLogic lowerConditionLogic;

    @JsonProperty("MicroChangeableEnable")
    private Boolean microChangeableEnable;

    @JsonProperty("MicroForceOnStandby")
    private Boolean microForceOnStandby;
}
