// 완제품LOT(ProductLot) 조회 전용 서비스 - assignWorkToMachine()에서 자동 생성되므로 쓰기 API 없음
package com.mes.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.ProductLotDetailDto;
import com.mes.backend.dto.ProductLotSummaryDto;
import com.mes.backend.entity.Bom;
import com.mes.backend.entity.Product;
import com.mes.backend.entity.ProductLot;
import com.mes.backend.entity.WorkOrder;
import com.mes.backend.repository.ProductLotRepository;
import com.mes.backend.repository.QualityInspectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductLotService {

    private static final String INSPECTION_RESULT_OK = "OK";
    private static final String INSPECTION_RESULT_NG = "NG";

    private final ProductLotRepository productLotRepo;
    private final QualityInspectionRepository qualityInspectionRepo;

    @Transactional(readOnly = true)
    public List<ProductLotSummaryDto> search(String productLotNo, String productName, String workOrderNo, String lotStatus) {
        return productLotRepo.search(productLotNo, productName, workOrderNo, lotStatus).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductLotDetailDto getDetail(Long id) {
        ProductLot productLot = productLotRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("완제품LOT을 찾을 수 없습니다. ID: " + id));

        long inspectionCount = qualityInspectionRepo.countByProductLot_Id(id);
        long passCount = qualityInspectionRepo.countByProductLot_IdAndInspectionResult(id, INSPECTION_RESULT_OK);
        long failCount = qualityInspectionRepo.countByProductLot_IdAndInspectionResult(id, INSPECTION_RESULT_NG);

        return ProductLotDetailDto.builder()
                .id(productLot.getId())
                .productLotNo(productLot.getProductLotNo())
                .productName(productNameOf(productLot))
                .workOrderNo(workOrderNoOf(productLot))
                .currentQty(productLot.getCurrentQty())
                .lotStatus(productLot.getLotStatus())
                .lotCreatedAt(productLot.getLotCreatedAt())
                .inspectionCount(inspectionCount)
                .passCount(passCount)
                .failCount(failCount)
                .build();
    }

    private ProductLotSummaryDto toSummaryDto(ProductLot productLot) {
        return ProductLotSummaryDto.builder()
                .id(productLot.getId())
                .productLotNo(productLot.getProductLotNo())
                .productName(productNameOf(productLot))
                .workOrderNo(workOrderNoOf(productLot))
                .currentQty(productLot.getCurrentQty())
                .lotStatus(productLot.getLotStatus())
                .lotCreatedAt(productLot.getLotCreatedAt())
                .build();
    }

    private String productNameOf(ProductLot productLot) {
        WorkOrder workOrder = productLot.getWorkOrder();
        Bom bom = workOrder != null ? workOrder.getBom() : null;
        Product product = bom != null ? bom.getProduct() : null;
        return product != null ? product.getProductName() : null;
    }

    private String workOrderNoOf(ProductLot productLot) {
        WorkOrder workOrder = productLot.getWorkOrder();
        return workOrder != null ? workOrder.getWorkOrderNo() : null;
    }
}
