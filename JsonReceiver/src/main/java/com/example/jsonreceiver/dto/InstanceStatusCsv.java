package com.example.jsonreceiver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstanceStatusCsv {
    private String hostname; // HOSTNAME
    private String machineType; // MACHINE_TYPE (SystemInfo.csvのElType)
    private String region; // REGION
    private String currentType; // CURRENT_TYPE (現在のインスタンスタイプ)
    private String typeId; // TYPE_ID (InstanceTypeLinkCsv.csvのInstanceTypeId)
    private String typeHigh; // TYPE_HIGH (InstanceType.csvのHighInstanceType)
    private String typeSmallStandard; // TYPE_SMALL_STANDARD (InstanceType.csvのLowInstanceType)
    private String typeMicro; // TYPE_MICRO (InstanceType.csvのVeryLowInstanceType)
    private String lastUpdate; // LASTUPDATE (最終更新時刻)
    private InstanceStatusValue agentStatus; // AGENT_STATUS (エージェント状態)
    private String agentVersion; // AGENT_VERSION (エージェントバージョン)
    private String agentLastNoticeTime; // AGENT_LAST_NOTICE_TIME (最終通知受信時刻)
}
