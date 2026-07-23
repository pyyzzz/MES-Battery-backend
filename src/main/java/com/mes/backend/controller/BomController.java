// 제품별 BOM 항목 일괄 조회/저장 API
package com.mes.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mes.backend.dto.BomItemDto;
import com.mes.backend.dto.BomItemSaveRequest;
import com.mes.backend.service.BomService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mes/products/{productId}/bom-items")
@RequiredArgsConstructor
public class BomController {

    private final BomService bomService;

    @GetMapping
    public List<BomItemDto> getBomItems(@PathVariable Long productId) {
        return bomService.getBomItems(productId);
    }

    @PutMapping
    public List<BomItemDto> saveAll(@PathVariable Long productId, @RequestBody List<BomItemSaveRequest> requests) {
        return bomService.saveAll(productId, requests);
    }
}
