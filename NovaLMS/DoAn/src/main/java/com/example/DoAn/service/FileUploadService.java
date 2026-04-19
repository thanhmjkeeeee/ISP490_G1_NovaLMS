package com.example.DoAn.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class FileUploadService {

    private final Cloudinary cloudinary;
    public FileUploadService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {

        System.out.println("Đang kết nối Cloudinary với tên: " + cloudName);

        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    public String uploadFile(MultipartFile file, String type) {
        try {
            String originalFilename = file.getOriginalFilename();
            Map<String, Object> params = new HashMap<>();
            
            // Đối với file tài liệu .docx, Cloudinary cần resource_type: raw để không bị strip extension
            if (originalFilename != null && originalFilename.toLowerCase().endsWith(".docx")) {
                params.put("resource_type", "raw");
                
                String fileNameWithoutExt = originalFilename.substring(0, originalFilename.lastIndexOf("."));
                String cleanName = fileNameWithoutExt.replaceAll("[^a-zA-Z0-9-]", "_");
                
                // Quan trọng: public_id của 'raw' phải chứa cả extension
                params.put("public_id", cleanName + "_" + System.currentTimeMillis() + ".docx");
            } else {
                params.put("resource_type", "auto");
                params.put("use_filename", true);
                params.put("unique_filename", true);
            }

            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            String url = uploadResult.get("secure_url").toString();
            return url;

        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tải " + type + " lên Cloudinary", e);
        }
    }

    public String upload(MultipartFile file) {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("resource_type", "video")
            );
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tải file lên Cloudinary", e);
        }
    }

    public String uploadBytes(byte[] bytes, String originalFilename, String resourceType) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("resource_type", resourceType); // "auto", "video", "raw"
            params.put("use_filename", true);
            params.put("unique_filename", true);
            
            if (originalFilename != null) {
                params.put("public_id", originalFilename.replaceAll("[^a-zA-Z0-9-]", "_") + "_" + System.currentTimeMillis());
            }

            Map<?, ?> uploadResult = cloudinary.uploader().upload(bytes, params);
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi tải dữ liệu lên Cloudinary", e);
        }
    }
}