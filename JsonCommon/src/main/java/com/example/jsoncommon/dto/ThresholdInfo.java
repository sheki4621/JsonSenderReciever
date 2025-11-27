package com.example.jsoncommon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdInfo {
    public ThresholdInfo(String string, ConditionLogic valueOf, double double1, double double2, int int1, int int2,
            ConditionLogic valueOf2, double double3, double double4, int int3, int int4, boolean boolean1,
            boolean boolean2, boolean boolean3, boolean boolean4) {
    }

    private String hostname;
    private ScalingMode scalingMode;
    private Boolean upperChangeableEnable;
    private Double upperCpuThreshold;
    private Integer upperCpuDurationMin;
    private Double upperMemThreshold;
    private Integer upperMemDurationMin;
    private ConditionLogic upperConditionLogic;
    private Boolean lowerChangeableEnable;
    private Double lowerCpuThreshold;
    private Integer lowerCpuDurationMin;
    private Double lowerMemThreshold;
    private Integer lowerMemDurationMin;
    private ConditionLogic lowerConditionLogic;
    private Boolean microChangeableEnable;
    private Boolean microForceOnStandby;
}
