package com.example.DoAn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResultResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ServiceResultResponse<T> success(String message) {
        return new ServiceResultResponse<>(true, message, null);
    }

    public static <T> ServiceResultResponse<T> success(String message, T data) {
        return new ServiceResultResponse<>(true, message, data);
    }

    public static <T> ServiceResultResponse<T> failure(String message) {
        return new ServiceResultResponse<>(false, message, null);
    }
}