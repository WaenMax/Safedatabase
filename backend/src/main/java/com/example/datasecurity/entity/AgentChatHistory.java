package com.example.datasecurity.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AgentChatHistory {
    private Long chatId;
    private Long userId;
    private String question;
    private String answer;
    private LocalDateTime createdTime;
}
