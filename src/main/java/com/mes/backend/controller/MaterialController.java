// 자재(Material) 마스터 CRUD API
package com.mes.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mes.backend.dto.MaterialCreateRequest;
import com.mes.backend.dto.MaterialUpdateRequest;
import com.mes.backend.entity.Material;
import com.mes.backend.service.MaterialService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mes/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public List<Material> search(@RequestParam(required = false) String materialName,
                                  @RequestParam(required = false) String materialCode) {
        return materialService.search(materialName, materialCode);
    }

    @GetMapping("/{id}")
    public Material getById(@PathVariable Long id) {
        return materialService.getById(id);
    }

    @PostMapping
    public Material create(@RequestBody MaterialCreateRequest request) {
        return materialService.create(request);
    }

    @PutMapping("/{id}")
    public Material update(@PathVariable Long id, @RequestBody MaterialUpdateRequest request) {
        return materialService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return ResponseEntity.ok().build();
    }
}
