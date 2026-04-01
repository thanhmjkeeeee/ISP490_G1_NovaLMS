package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class FileUploadApiController {

    private final FileUploadService fileUploadService;

    @PostMapping("/{type}")
    public ResponseData<String> uploadFile(
            @PathVariable String type,
            @RequestParam("file") MultipartFile file) {
        
        if (file == null || file.isEmpty()) {
            return ResponseData.error(400, "Vui lòng chọn file để tải lên.");
        }

        // Kiểm tra dung lượng (Audio 20MB, Image 5MB, Document 10MB)
        long maxSize;
        if (type.equals("audio")) {
            maxSize = 20 * 1024 * 1024;
        } else if (type.equals("document")) {
            maxSize = 10 * 1024 * 1024;
        } else {
            maxSize = 5 * 1024 * 1024;
        }

        if (file.getSize() > maxSize) {
            return ResponseData.error(400, "Dung lượng file vượt quá giới hạn cho phép (" + (maxSize / 1024 / 1024) + "MB).");
        }

        try {
            String url = fileUploadService.uploadFile(file, type);
            return ResponseData.success("Tải lên thành công", url);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi khi tải file: " + e.getMessage());
        }
    }
}
