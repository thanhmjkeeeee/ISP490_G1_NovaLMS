package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.Setting;
import com.example.DoAn.service.SettingService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
public class SettingApiController {

    private final SettingService settingService;

    public SettingApiController(SettingService settingService) {
        this.settingService = settingService;
    }

    // GET /api/settings?type=COURSE_CATEGORY
    @GetMapping
    public ResponseEntity<ResponseData<List<Setting>>> getSettings(@RequestParam(required = false, defaultValue = "COURSE_CATEGORY") String type) {
        List<Setting> settings = settingService.getSettingsByType(type);
        return ResponseEntity.ok(new ResponseData<>(HttpStatus.OK.value(), "Success", settings));
    }

    // POST /api/settings
    @PostMapping
    public ResponseEntity<ResponseData<Setting>> createSetting(@RequestBody SettingRequest request) {
        try {
            Setting setting = settingService.createSetting(
                    request.getSettingType(),
                    request.getName(),
                    request.getValue(),
                    request.getDescription(),
                    request.getOrderIndex()
            );
            return ResponseEntity.ok(new ResponseData<>(HttpStatus.CREATED.value(), "Setting created successfully", setting));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseData<>(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
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
            return ResponseEntity.ok(new ResponseData<>(HttpStatus.OK.value(), "Category updated successfully", setting));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseData<>(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    // DELETE /api/settings/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<Void>> deleteSetting(@PathVariable Integer id) {
        try {
            settingService.deleteSetting(id);
            return ResponseEntity.ok(new ResponseData<>(HttpStatus.OK.value(), "Category deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ResponseData<>(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }

    // DTO mapping payload
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SettingRequest {
        private String settingType;
        private String name;
        private String value;
        private String description;
        private Integer orderIndex;
        private String status;

        public String getSettingType() { return settingType; }
        public void setSettingType(String settingType) { this.settingType = settingType; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Integer getOrderIndex() { return orderIndex; }
        public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
