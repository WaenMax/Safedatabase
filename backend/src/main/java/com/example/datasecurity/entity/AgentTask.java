package com.example.datasecurity.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AgentTask {
    private Long taskId;
    private String taskType;
    private String taskStatus;
    private String inputData;
    private String outputData;
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime finishedTime;
    private String errorMessage;
}
