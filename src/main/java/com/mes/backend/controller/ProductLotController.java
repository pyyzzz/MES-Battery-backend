// 완제품LOT 조회 전용 API (쓰기 없음 - ProductLot은 assignWorkToMachine()에서 자동 생성됨)
package com.mes.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mes.backend.dto.ProductLotDetailDto;
import com.mes.backend.dto.ProductLotSummaryDto;
import com.mes.backend.service.ProductLotService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mes/product-lots")
@RequiredArgsConstructor
public class ProductLotController {

    private final ProductLotService productLotService;

    @GetMapping
    public List<ProductLotSummaryDto> search(@RequestParam(required = false) String productLotNo,
                                              @RequestParam(required = false) String productName,
                                              @RequestParam(required = false) String workOrderNo,
                                              @RequestParam(required = false) String lotStatus) {
        return productLotService.search(productLotNo, productName, workOrderNo, lotStatus);
    }

    @GetMapping("/{id}")
    public ProductLotDetailDto getDetail(@PathVariable Long id) {
        return productLotService.getDetail(id);
    }
}
