package com.example.DoAn.dto;

import com.example.DoAn.dto.CourseDTO;
import com.example.DoAn.dto.UserProfileDTO;

public class ResponseDTO {
    private boolean success;
    private String message;
    private UserProfileDTO userProfile;
    private CourseDTO course;
    private Object data;
}