package com.example.DoAn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseData<T> {
    private int status;
    private String message;
    private T data;

    public static <T> ResponseData<T> success(String message, T data) {
        return new ResponseData<>(200, message, data);
    }

    public static <T> ResponseData<T> success(String message) {
        return new ResponseData<>(200, message, null);
    }

    public static <T> ResponseData<T> error(int status, String message) {
        return new ResponseData<>(status, message, null);
    }
}