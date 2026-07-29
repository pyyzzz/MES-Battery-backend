package com.mes.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NotificationDto {
    private String id;
    private String type;
    private String severity;
    private String title;
    private String message;
    private String time;
}
