package com.mes.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mes.backend.dto.inventory.InventoryLotDto;
import com.mes.backend.dto.inventory.InventoryLotSummaryDto;
import com.mes.backend.dto.inventory.InventoryMaterialDto;
import com.mes.backend.dto.inventory.InventoryMaterialSummaryDto;
import com.mes.backend.dto.inventory.InventoryTransactionDto;
import com.mes.backend.dto.inventory.InventoryTransactionSummaryDto;
import com.mes.backend.dto.inventory.MaterialInboundRequest;
import com.mes.backend.service.InventoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mes/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("/materials")
    public List<InventoryMaterialDto> getMaterials(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return inventoryService.getMaterials(startDate, endDate, status, keyword);
    }

    @GetMapping("/materials/summary")
    public InventoryMaterialSummaryDto getMaterialSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return inventoryService.getMaterialSummary(startDate, endDate, status, keyword);
    }

    @PostMapping("/materials/{materialId}/inbound")
    public InventoryMaterialDto inboundMaterial(
            @PathVariable Long materialId,
            @RequestBody MaterialInboundRequest request
    ) {
        return inventoryService.inboundMaterial(materialId, request.getQuantity(), request.getEmployeeId());
    }

    @GetMapping("/transactions")
    public List<InventoryTransactionDto> getTransactions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword
    ) {
        return inventoryService.getTransactions(startDate, endDate, type, keyword);
    }

    @GetMapping("/transactions/summary")
    public InventoryTransactionSummaryDto getTransactionSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword
    ) {
        return inventoryService.getTransactionSummary(startDate, endDate, type, keyword);
    }

    @GetMapping("/lots")
    public List<InventoryLotDto> getLots(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return inventoryService.getLots(startDate, endDate, status, keyword);
    }

    @GetMapping("/lots/summary")
    public InventoryLotSummaryDto getLotSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return inventoryService.getLotSummary(startDate, endDate, status, keyword);
    }
}
