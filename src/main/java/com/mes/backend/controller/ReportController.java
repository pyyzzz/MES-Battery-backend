package com.mes.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mes.backend.dto.report.ReportDailyProductionDto;
import com.mes.backend.dto.report.ReportLotDto;
import com.mes.backend.dto.report.ReportProcessChartDto;
import com.mes.backend.dto.report.ReportSummaryDto;
import com.mes.backend.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mes/report")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/lots")
    public List<ReportLotDto> getLots(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String process,
            @RequestParam(required = false) String keyword
    ) {
        return reportService.getLots(startDate, endDate, product, equipment, process, keyword);
    }

    @GetMapping("/summary")
    public ReportSummaryDto getSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String process,
            @RequestParam(required = false) String keyword
    ) {
        return reportService.getSummary(startDate, endDate, product, equipment, process, keyword);
    }

    @GetMapping("/daily")
    public List<ReportDailyProductionDto> getDailyProduction(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String process,
            @RequestParam(required = false) String keyword
    ) {
        return reportService.getDailyProduction(startDate, endDate, product, equipment, process, keyword);
    }

    @GetMapping("/process")
    public List<ReportProcessChartDto> getProcessProduction(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String process,
            @RequestParam(required = false) String keyword
    ) {
        return reportService.getProcessProduction(startDate, endDate, product, equipment, process, keyword);
    }
}
