package com.example.DoAn.service;

import com.example.DoAn.dto.response.ProfileResponseDTO;
import com.example.DoAn.dto.request.ProfileRequestDTO;
import com.example.DoAn.dto.response.ResponseData;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    ResponseData<ProfileResponseDTO> getUserProfile(String email);
    ResponseData<Void> updateUserProfile(String email, ProfileRequestDTO dto);
    ResponseData<String> updateAvatar(String email, MultipartFile file);
}