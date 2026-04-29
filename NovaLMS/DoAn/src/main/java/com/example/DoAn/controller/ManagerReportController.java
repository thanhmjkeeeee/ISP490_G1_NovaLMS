package com.example.DoAn.controller;

import com.example.DoAn.service.ExcelExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/manager/reports")
@RequiredArgsConstructor
public class ManagerReportController {

    private final ExcelExportService excelExportService;

    @GetMapping("/export/registrations")
    public ResponseEntity<InputStreamResource> exportRegistrations() {
        var in = excelExportService.exportRegistrationsToExcel();

        String filename = "Registrations_Report_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + filename);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @GetMapping("/export/revenue")
    public ResponseEntity<InputStreamResource> exportRevenue() {
        var in = excelExportService.exportRevenueToExcel();

        String filename = "Revenue_Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + filename);

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}
