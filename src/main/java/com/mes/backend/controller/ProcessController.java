// 공정(Process) 마스터 CRUD API
package com.mes.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mes.backend.dto.ProcessCreateRequest;
import com.mes.backend.dto.ProcessUpdateRequest;
import com.mes.backend.entity.Process;
import com.mes.backend.service.ProcessService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mes/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;

    @GetMapping
    public List<Process> search(@RequestParam(required = false) String processName,
                                 @RequestParam(required = false) String processStatus) {
        return processService.search(processName, processStatus);
    }

    @GetMapping("/{id}")
    public Process getById(@PathVariable Long id) {
        return processService.getById(id);
    }

    @PostMapping
    public Process create(@RequestBody ProcessCreateRequest request) {
        return processService.create(request);
    }

    @PutMapping("/{id}")
    public Process update(@PathVariable Long id, @RequestBody ProcessUpdateRequest request) {
        return processService.update(id, request);
    }
}
