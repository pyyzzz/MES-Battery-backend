package com.mes.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mes.backend.dto.ProductionReportDto;
import com.mes.backend.entity.WorkOrder;
import com.mes.backend.service.EquipmentService;
import com.mes.backend.service.ProductionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mes")
@RequiredArgsConstructor
public class MesController {

    private final ProductionService productionService;
    private final EquipmentService equipmentService;

    @GetMapping("/machine/poll")
    public ResponseEntity<WorkOrder> pollWorkOrder(@RequestParam String machineId) {
        WorkOrder order = productionService.assignWorkToMachine(machineId);
        return (order != null) ? ResponseEntity.ok(order) : ResponseEntity.noContent().build();
    }

    @PostMapping("/machine/report")
    public ResponseEntity<Void> reportProduction(@RequestBody ProductionReportDto request) {
        productionService.reportProduction(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/machine/environment")
    public ResponseEntity<Void> reportEnvironment(@RequestBody ProductionReportDto request) {
        equipmentService.reportEnvironment(request);
        return ResponseEntity.ok().build();
    }
}
