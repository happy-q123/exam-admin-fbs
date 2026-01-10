package com.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
@Builder
@Data
@AllArgsConstructor
public class StompMessageDto {
    private String message;
    private String senderId;
    private String receiverId;
    private String destination;
}
