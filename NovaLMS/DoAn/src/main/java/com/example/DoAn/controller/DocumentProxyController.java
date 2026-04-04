package com.example.DoAn.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
@RequestMapping("/api/v1/files")
public class DocumentProxyController {

    @GetMapping("/preview")
    public ResponseEntity<InputStreamResource> previewDocument(@RequestParam String url) {
        try {
            if (url == null || !url.toLowerCase().contains(".pdf")) {
                return ResponseEntity.badRequest().build();
            }

            // 1. Tạo kết nối HTTP chuẩn (Bypass các lỗi của RestTemplate)
            URL fileUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) fileUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            // Lấy độ dài file thực tế từ Cloudinary
            int contentLength = connection.getContentLength();
            InputStream inputStream = connection.getInputStream();

            String filename = url.substring(url.lastIndexOf("/") + 1);
            if (filename.contains("?")) {
                filename = filename.substring(0, filename.indexOf("?"));
            }

            // 2. Trả về InputStreamResource để Stream dữ liệu trực tiếp
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    // Bắt buộc khai báo Content-Length để Chrome biết đường vẽ PDF
                    .contentLength(contentLength > 0 ? contentLength : -1)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(new InputStreamResource(inputStream));

        } catch (Exception e) {
            System.err.println("LỖI PROXY PDF: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}