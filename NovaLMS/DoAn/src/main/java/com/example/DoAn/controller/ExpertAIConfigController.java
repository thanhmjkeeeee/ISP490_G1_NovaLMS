package com.example.DoAn.controller;

import com.example.DoAn.model.AIPromptConfig;
import com.example.DoAn.service.IAIPromptConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/expert/ai-config")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ROLE_EXPERT', 'ROLE_ADMIN')")
public class ExpertAIConfigController {

    private final IAIPromptConfigService aiPromptConfigService;

    @GetMapping
    public String showConfigPage(org.springframework.ui.Model model) {
        model.addAttribute("activePage", "ai-config");
        return "expert/ai-config";
    }

    @GetMapping("/api/{bucket}")
    @ResponseBody
    public ResponseEntity<?> getConfig(@PathVariable String bucket) {
        AIPromptConfig config = aiPromptConfigService.getConfigByBucket(bucket);
        if (config == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(config);
    }

    @PostMapping("/api/save")
    @ResponseBody
    public ResponseEntity<?> saveConfig(@RequestBody AIPromptConfig config) {
        try {
            // Only allow updating existing buckets
            AIPromptConfig existing = aiPromptConfigService.getConfigByBucket(config.getBucket());
            if (existing != null) {
                config.setId(existing.getId());
            }
            aiPromptConfigService.saveConfig(config);
            return ResponseEntity.ok(Map.of("message", "Cập nhật cấu hình AI thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    @PostMapping("/api/reset/{bucket}")
    @ResponseBody
    public ResponseEntity<?> resetConfig(@PathVariable String bucket) {
        try {
            aiPromptConfigService.resetToDefault(bucket);
            return ResponseEntity.ok(Map.of("message", "Đã khôi phục cấu hình mặc định cho " + bucket));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }
}
