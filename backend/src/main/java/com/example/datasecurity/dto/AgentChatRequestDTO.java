package com.example.datasecurity.dto;

import lombok.Data;

@Data
public class AgentChatRequestDTO {
    private String question;
    private String provider;
    private String model;
}
