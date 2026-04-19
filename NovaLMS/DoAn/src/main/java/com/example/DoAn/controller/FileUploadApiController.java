package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class FileUploadApiController {

    private final FileUploadService fileUploadService;

    private static final Set<String> AUDIO_EXTENSIONS = Set.of(
            "mp3", "wav", "ogg", "m4a", "aac", "opus", "flac", "webm");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of(
            "doc", "docx", "pdf", "ppt", "pptx", "xls", "xlsx", "txt");

    @PostMapping("/{type}")
    public ResponseData<String> uploadFile(
            @PathVariable String type,
            @RequestParam("file") MultipartFile file) {
        
        if (file == null || file.isEmpty()) {
            return ResponseData.error(400, "Vui lòng chọn file để tải lên.");
        }

        String typeError = validateMimeAndExtension(type, file);
        if (typeError != null) {
            return ResponseData.error(400, typeError);
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

    /**
     * Kiểm tra MIME và phần mở rộng khớp với loại upload (audio / image / document).
     */
    private static String validateMimeAndExtension(String type, MultipartFile file) {
        String mime = file.getContentType();
        if (mime != null) {
            mime = mime.toLowerCase(Locale.ROOT);
        }
        String ext = extensionOf(file.getOriginalFilename());

        switch (type) {
            case "audio":
                if (mime != null && mime.startsWith("audio/")) {
                    return null;
                }
                if (ext != null && AUDIO_EXTENSIONS.contains(ext)) {
                    return null;
                }
                return "File không phải âm thanh hợp lệ. Chọn định dạng mp3, wav, ogg, m4a, ...";
            case "image":
                if (mime != null && mime.startsWith("image/")) {
                    return null;
                }
                if (ext != null && IMAGE_EXTENSIONS.contains(ext)) {
                    return null;
                }
                return "File không phải ảnh hợp lệ. Chọn jpg, png, gif, webp, ...";
            case "document":
                if (mime != null && (
                        mime.contains("pdf")
                                || mime.contains("msword")
                                || mime.contains("wordprocessingml")
                                || mime.contains("spreadsheetml")
                                || mime.contains("presentationml")
                                || mime.contains("officedocument")
                                || "text/plain".equals(mime))) {
                    return null;
                }
                if (ext != null && DOCUMENT_EXTENSIONS.contains(ext)) {
                    return null;
                }
                return "File không phải tài liệu hợp lệ (docx, pdf, ...).";
            default:
                return null;
        }
    }

    private static String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
