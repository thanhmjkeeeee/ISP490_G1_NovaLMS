package com.example.DoAn.controller;

import com.example.DoAn.dto.response.RescheduleResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.RescheduleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/manager/reschedule")
@RequiredArgsConstructor
public class ManagerRescheduleController {

    private final RescheduleService rescheduleService;

    @GetMapping("/list")
    public String listPage() {
        return "manager/reschedule-list";
    }

    @GetMapping("/api/requests")
    @ResponseBody
    public ResponseData<Page<RescheduleResponseDTO>> getRequests(
                                                                  @RequestParam(required = false) String teacherName,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        return rescheduleService.getRequestsForManager(teacherName, status, page, size);
    }

    @PutMapping("/api/requests/{id}/status")
    @ResponseBody
    public ResponseData<Void> updateStatus(
            @PathVariable Integer id,
            @RequestBody StatusUpdateDTO statusUpdate) {
        return rescheduleService.updateStatus(id, statusUpdate.getStatus(), statusUpdate.getManagerNote());
    }

    @Data
    public static class StatusUpdateDTO {
        private String status;
        private String managerNote;
    }
}
