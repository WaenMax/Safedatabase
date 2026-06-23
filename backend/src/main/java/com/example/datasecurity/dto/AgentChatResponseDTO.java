package com.example.datasecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatResponseDTO {
    private String question;
    private String answer;
    private String provider;
    private String model;
}
