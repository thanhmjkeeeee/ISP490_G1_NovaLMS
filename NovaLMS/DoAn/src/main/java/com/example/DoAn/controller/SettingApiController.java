package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.dto.response.SettingDTO;
import com.example.DoAn.model.Setting;
import com.example.DoAn.service.SettingService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/settings")
public class SettingApiController {

    private final SettingService settingService;

    public SettingApiController(SettingService settingService) {
        this.settingService = settingService;
    }

    // GET /api/settings?type=COURSE_CATEGORY&activeOnly=true
    @GetMapping
    public ResponseEntity<ResponseData<List<SettingDTO>>> getSettings(
            @RequestParam(required = false, defaultValue = "COURSE_CATEGORY") String type,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        try {
            System.out.println(">>> [LOG] Fetching settings for type: " + type + ", activeOnly=" + activeOnly);
            List<Setting> entities = settingService.getSettingsByType(type, activeOnly);
            
            // Chuyển đổi sang DTO để loại bỏ hoàn toàn lỗi JSON Serialization
            List<SettingDTO> dtos = entities.stream().map(s -> SettingDTO.builder()
                    .settingId(s.getSettingId())
                    .name(s.getName())
                    .value(s.getValue())
                    .settingType(s.getSettingType())
                    .orderIndex(s.getOrderIndex())
                    .status(s.getStatus())
                    .description(s.getDescription())
                    .build()).collect(Collectors.toList());

            System.out.println(">>> [LOG] Successfully mapped " + dtos.size() + " items to DTO.");
            return ResponseEntity.ok(ResponseData.success("Success", dtos));
        } catch (Exception e) {
            System.err.println(">>> [ERROR] Failed to fetch settings: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ResponseData<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Server Error: " + e.getMessage()));
        }
    }

    // POST /api/settings
    @PostMapping
    public ResponseEntity<ResponseData<Setting>> addSetting(@RequestBody SettingRequest request) {
        try {
            Setting setting = settingService.createSetting(
                request.getSettingType(),
                request.getName(),
                request.getValue(),
                request.getDescription(),
                request.getOrderIndex()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(ResponseData.success("Thêm thành công!", setting));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseData.error(HttpStatus.BAD_REQUEST.value(), "Lỗi: " + e.getMessage()));
        }
    }

    // PUT /api/settings/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ResponseData<Setting>> updateSetting(@PathVariable Integer id, @RequestBody SettingRequest request) {
        try {
            Setting setting = settingService.updateSetting(
                id,
                request.getName(),
                request.getValue(),
                request.getDescription(),
                request.getOrderIndex(),
                request.getStatus()
            );
            return ResponseEntity.ok(ResponseData.success("Cập nhật thành công!", setting));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseData.error(HttpStatus.BAD_REQUEST.value(), "Lỗi: " + e.getMessage()));
        }
    }

    // DELETE /api/settings/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<Void>> deleteSetting(@PathVariable Integer id) {
        try {
            settingService.deleteSetting(id);
            return ResponseEntity.ok(ResponseData.success("Xóa thành công!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseData.error(HttpStatus.BAD_REQUEST.value(), "Lỗi: " + e.getMessage()));
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SettingRequest {
        private String name;
        private String value;
        private String settingType;
        private String description;
        private Integer orderIndex;
        private String status;
        
        // Manual Getters/Setters if Lombok fails
        public String getName() { return name; }
        public String getValue() { return value; }
        public String getSettingType() { return settingType; }
        public String getDescription() { return description; }
        public Integer getOrderIndex() { return orderIndex; }
        public String getStatus() { return status; }
    }
}
