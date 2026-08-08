// BOM(제품별 소요자재) 일괄 저장 서비스 - 화면에서 자유 편집 후 저장 시 한 번에 반영
package com.mes.backend.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.BomItemDto;
import com.mes.backend.dto.BomItemSaveRequest;
import com.mes.backend.entity.Bom;
import com.mes.backend.entity.BomItem;
import com.mes.backend.entity.Material;
import com.mes.backend.entity.Process;
import com.mes.backend.exception.CustomException;
import com.mes.backend.repository.BomItemRepository;
import com.mes.backend.repository.BomRepository;
import com.mes.backend.repository.MaterialRepository;
import com.mes.backend.repository.ProcessRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BomService {

    private final BomRepository bomRepo;
    private final BomItemRepository bomItemRepo;
    private final MaterialRepository materialRepo;
    private final ProcessRepository processRepo;

    public List<BomItemDto> getBomItems(Long productId) {
        Bom bom = findBomByProductId(productId);
        return bomItemRepo.findAllByBom(bom).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public List<BomItemDto> saveAll(Long productId, List<BomItemSaveRequest> requests) {
        validateNoDuplicates(requests);

        Bom bom = findBomByProductId(productId);
        List<BomItem> existingItems = bomItemRepo.findAllByBom(bom);
        Map<Long, BomItem> existingById = existingItems.stream()
                .collect(Collectors.toMap(BomItem::getId, item -> item));

        int nextSequence = existingItems.stream()
                .map(BomItem::getSequenceNo)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max().orElse(0) + 1;

        Set<Long> keepIds = new HashSet<>();
        List<BomItem> toSave = new ArrayList<>();

        for (BomItemSaveRequest request : requests) {
            if (request.getId() != null) {
                BomItem existing = existingById.get(request.getId());
                if (existing == null) {
                    throw new RuntimeException("BomItem을 찾을 수 없습니다. ID: " + request.getId());
                }
                existing.setInputProcess(findProcess(request.getInputProcessId()));
                existing.setRequiredQuantity(request.getRequiredQuantity());
                toSave.add(existing);
                keepIds.add(existing.getId());
            } else {
                BomItem newItem = BomItem.builder()
                        .bom(bom)
                        .material(findMaterial(request.getMaterialId()))
                        .inputProcess(findProcess(request.getInputProcessId()))
                        .sequenceNo(nextSequence++)
                        .requiredQuantity(request.getRequiredQuantity())
                        .build();
                toSave.add(newItem);
            }
        }

        List<BomItem> toDelete = existingItems.stream()
                .filter(item -> !keepIds.contains(item.getId()))
                .collect(Collectors.toList());
        bomItemRepo.deleteAll(toDelete);

        List<BomItem> saved = bomItemRepo.saveAll(toSave);
        return saved.stream().map(this::toDto).collect(Collectors.toList());
    }

    /* 요청 목록 안에서 material_id+input_process_id 조합 중복 검증 (DB 반영 전에 먼저 확인) */
    private void validateNoDuplicates(List<BomItemSaveRequest> requests) {
        Set<String> seen = new HashSet<>();
        for (BomItemSaveRequest request : requests) {
            String key = request.getMaterialId() + ":" + request.getInputProcessId();
            if (!seen.add(key)) {
                throw new CustomException("DUPLICATE_BOM_ITEM", "같은 자재+같은 공정 조합은 한 행만 허용됩니다.");
            }
        }
    }

    private Bom findBomByProductId(Long productId) {
        return bomRepo.findByProduct_Id(productId)
                .orElseThrow(() -> new RuntimeException("해당 제품의 BOM을 찾을 수 없습니다. productId: " + productId));
    }

    private Material findMaterial(Long materialId) {
        return materialRepo.findById(materialId)
                .orElseThrow(() -> new RuntimeException("자재를 찾을 수 없습니다. ID: " + materialId));
    }

    private Process findProcess(Long processId) {
        return processRepo.findById(processId)
                .orElseThrow(() -> new RuntimeException("공정을 찾을 수 없습니다. ID: " + processId));
    }

    private BomItemDto toDto(BomItem item) {
        return new BomItemDto(
                item.getId(),
                item.getMaterial().getId(),
                item.getMaterial().getMaterialCode(),
                item.getMaterial().getMaterialName(),
                item.getMaterial().getUnit(),
                item.getRequiredQuantity(),
                item.getInputProcess().getId(),
                item.getInputProcess().getProcessName());
    }
}
