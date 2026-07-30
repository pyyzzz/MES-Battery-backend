// 설비(Equipment) 마스터 CRUD API
package com.mes.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mes.backend.dto.EquipmentCreateRequest;
import com.mes.backend.dto.EquipmentUpdateRequest;
import com.mes.backend.entity.Equipment;
import com.mes.backend.service.EquipmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mes/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping
    public List<Equipment> search(@RequestParam(required = false) String equipmentName,
                                   @RequestParam(required = false) String equipmentStatus) {
        return equipmentService.search(equipmentName, equipmentStatus);
    }

    @GetMapping("/{id}")
    public Equipment getById(@PathVariable Long id) {
        return equipmentService.getById(id);
    }

    @PostMapping
    public Equipment create(@RequestBody EquipmentCreateRequest request) {
        return equipmentService.create(request);
    }

    @PutMapping("/{id}")
    public Equipment update(@PathVariable Long id, @RequestBody EquipmentUpdateRequest request) {
        return equipmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        equipmentService.delete(id);
    }
}
