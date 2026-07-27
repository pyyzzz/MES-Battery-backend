// 자재(Material) 마스터 CRUD - 재고/LOT은 다루지 않음(MaterialLot 쪽 별개 담당)
package com.mes.backend.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.MaterialCreateRequest;
import com.mes.backend.dto.MaterialUpdateRequest;
import com.mes.backend.entity.Material;
import com.mes.backend.repository.MaterialRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepo;

    public List<Material> search(String materialName, String materialCode) {
        return materialRepo.search(materialName, materialCode);
    }

    public Material getById(Long id) {
        return materialRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("자재를 찾을 수 없습니다. ID: " + id));
    }

    /* registered_at은 명시적으로 안 채움 - Material의 @PrePersist가 null이면 현재시각으로 자동 세팅 */
    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public Material create(MaterialCreateRequest request) {
        return materialRepo.save(Material.builder()
                .materialCode(request.getMaterialCode())
                .materialName(request.getMaterialName())
                .unit(request.getUnit())
                .safetyStock(request.getSafetyStock())
                .build());
    }

    /* material_code는 잠금 - 여기서 건드리지 않음 */
    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public Material update(Long id, MaterialUpdateRequest request) {
        Material material = getById(id);
        material.setMaterialName(request.getMaterialName());
        material.setUnit(request.getUnit());
        material.setSafetyStock(request.getSafetyStock());
        return materialRepo.save(material);
    }

    /* 실제 삭제 대신 is_active=false로 soft delete */
    @PreAuthorize("hasAuthority('관리자')")
    @Transactional
    public void delete(Long id) {
        Material material = getById(id);
        material.setActive(false);
        materialRepo.save(material);
    }
}
