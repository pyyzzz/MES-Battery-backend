package com.mes.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mes.backend.dto.quality.QualityDefectDistributionDto;
import com.mes.backend.dto.quality.QualityInspectionDto;
import com.mes.backend.dto.quality.QualitySummaryDto;
import com.mes.backend.dto.quality.QualityTrendDto;
import com.mes.backend.service.QualityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mes/quality")
@RequiredArgsConstructor
public class QualityController {
    private final QualityService qualityService;

    @GetMapping("/inspections")
    public List<QualityInspectionDto> getInspections(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String defectType,
            @RequestParam(required = false) String keyword
    ) {
        return qualityService.getInspections(startDate, endDate, result, defectType, keyword);
    }

    @GetMapping("/summary")
    public QualitySummaryDto getSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String defectType,
            @RequestParam(required = false) String keyword
    ) {
        return qualityService.getSummary(startDate, endDate, result, defectType, keyword);
    }

    @GetMapping("/trend")
    public List<QualityTrendDto> getTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String defectType,
            @RequestParam(required = false) String keyword
    ) {
        return qualityService.getTrend(startDate, endDate, result, defectType, keyword);
    }

    @GetMapping("/defects")
    public List<QualityDefectDistributionDto> getDefects(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String defectType,
            @RequestParam(required = false) String keyword
    ) {
        return qualityService.getDefectDistribution(startDate, endDate, result, defectType, keyword);
    }
}
