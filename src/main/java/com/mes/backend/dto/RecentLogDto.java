package com.mes.backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecentLogDto {
    private Long id;
    private String serialNo;
    private String result;
    private String machineId;
    private LocalDateTime producedAt;
}
