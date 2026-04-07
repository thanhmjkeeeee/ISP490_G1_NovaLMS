package com.example.DoAn.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.io.Serializable;

@Getter
public class ResponseData<T> implements Serializable {

    private final int status;

    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    /**
     * Khởi tạo response có trả về dữ liệu (Thường dùng cho GET, POST)
     *
     * @param status  Mã trạng thái HTTP
     * @param message Thông báo kết quả
     * @param data    Dữ liệu trả về
     */
    public ResponseData(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    /**
     * Khởi tạo response không có dữ liệu (Thường dùng cho PUT, PATCH, DELETE hoặc khi có lỗi)
     *
     * @param status  Mã trạng thái HTTP
     * @param message Thông báo kết quả
     */
    public ResponseData(int status, String message) {
        this.status = status;
        this.message = message;
        this.data = null;
    }

    public static <T> ResponseData<T> success(String message, T data) {
        return new ResponseData<>(200, message, data);
    }

    public static <T> ResponseData<T> success(String message) {
        return new ResponseData<>(200, message);
    }

    public static <T> ResponseData<T> success(T data) {
        return new ResponseData<>(200, "OK", data);
    }

    public static <T> ResponseData<T> error(int status, String message) {
        return new ResponseData<>(status, message);
    }
}