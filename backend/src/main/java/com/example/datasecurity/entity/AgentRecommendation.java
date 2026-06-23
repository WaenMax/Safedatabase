package com.example.datasecurity.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AgentRecommendation {
    private Long recommendationId;
    private String recommendationType;
    private String targetType;
    private Long targetId;
    private String recommendationResult;
    private String riskLevel;
    private BigDecimal confidence;
    private String reason;
    private String suggestion;
    private Boolean applied;
    private LocalDateTime createdTime;
    private Long createdBy;
}
