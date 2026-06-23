package com.example.datasecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentClassifyResultDTO {
    private Long fieldId;
    private Long categoryId;
    private String categoryName;
    private Long levelId;
    private String levelCode;
    private String levelName;
    private String reason;
    private double confidence;
    private String suggestion;
    private boolean applied;
}
