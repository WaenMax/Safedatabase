package com.example.datasecurity.dto;

import lombok.Data;

@Data
public class RiskAlertDTO {
    private Long alertId;
    private String riskType;
    private String riskLevel;
    private Long userId;
    private String targetType;
    private Long targetId;
    private String description;
    private String suggestion;
    private String status;
}
