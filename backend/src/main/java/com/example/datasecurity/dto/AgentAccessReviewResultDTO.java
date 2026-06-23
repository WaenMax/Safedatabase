package com.example.datasecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentAccessReviewResultDTO {
    private Long requestId;
    private String recommendation;
    private String riskLevel;
    private double confidence;
    private String reason;
    private String suggestion;
}
