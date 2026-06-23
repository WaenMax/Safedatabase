package com.example.datasecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatResponseDTO {
    private String question;
    private String answer;
    private String provider;
    private String model;
    private List<Map<String, Object>> references;
    private List<Map<String, Object>> actions;

    public AgentChatResponseDTO(String question, String answer, String provider, String model) {
        this(question, answer, provider, model, List.of(), List.of());
    }
}
