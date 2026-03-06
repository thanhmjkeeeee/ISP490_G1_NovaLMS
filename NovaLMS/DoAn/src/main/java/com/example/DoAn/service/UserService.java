package com.example.DoAn.service;

import com.example.DoAn.dto.ProfileResponseDTO;
import com.example.DoAn.dto.ProfileRequestDTO;
import com.example.DoAn.dto.ResponseData;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    ResponseData<ProfileResponseDTO> getUserProfile(String email);
    ResponseData<Void> updateUserProfile(String email, ProfileRequestDTO dto);
    ResponseData<String> updateAvatar(String email, MultipartFile file);
}