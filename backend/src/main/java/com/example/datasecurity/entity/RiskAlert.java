package com.example.datasecurity.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RiskAlert {
    private Long alertId;
    private String riskType;
    private String riskLevel;
    private Long userId;
    private String targetType;
    private Long targetId;
    private String description;
    private String suggestion;
    private String status;
    private LocalDateTime createdTime;
    private Long handledBy;
    private LocalDateTime handledTime;
}
