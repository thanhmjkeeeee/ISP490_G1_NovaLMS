package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/maintenance")
@RequiredArgsConstructor
public class MaintenanceApiController {

    private final MaintenanceService maintenanceService;

    @PostMapping("/cleanup")
    public ResponseData<Map<String, Long>> triggerCleanup() {
        return maintenanceService.performCleanup();
    }
}
