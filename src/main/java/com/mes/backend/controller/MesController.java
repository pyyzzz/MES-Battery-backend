package com.mes.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mes.backend.dto.MaterialInboundDto;
import com.mes.backend.dto.ProductionReportRequest;
import com.mes.backend.dto.RecentLogDto;
import com.mes.backend.dto.WorkOrderCreateRequest;
import com.mes.backend.entity.Material;
import com.mes.backend.entity.WorkOrder;
import com.mes.backend.service.ProductionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mes")
@RequiredArgsConstructor
public class MesController {

    private final ProductionService productionService;

    @PostMapping("/material/inbound")
    public ResponseEntity<Void> inboundMaterial(@RequestBody MaterialInboundDto request) {
        productionService.inboundMaterial(request.getCode(), request.getName(), request.getAmount());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/material/stock")
    public List<Material> getMaterialStock() {
        return productionService.getMaterialStock();
    }

    @PostMapping("/order")
    public WorkOrder createOrder(@RequestBody WorkOrderCreateRequest request) {
        return productionService.createWorkOrder(request.getProductCode(), request.getTargetQty());
    }

    @GetMapping("/orders")
    public List<WorkOrder> getAllOrders() {
        return productionService.getAllWorkOrders();
    }

    @GetMapping("/machine/poll")
    public ResponseEntity<WorkOrder> pollWorkOrder(@RequestParam String machineId) {
        WorkOrder order = productionService.assignWorkToMachine(machineId);
        return (order != null) ? ResponseEntity.ok(order) : ResponseEntity.noContent().build();
    }

    @PostMapping("/machine/report")
    public ResponseEntity<Void> reportProduction(@RequestBody ProductionReportRequest request) {
        productionService.reportProduction(
                request.getOrderId(), request.getMachineId(),
                request.getResult(), request.getDefectCode(), request.getSerialNo());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/production/recent-logs")
    public List<RecentLogDto> getRecentLogs() {
        return productionService.getRecentLogs();
    }
}
