package com.example.classregistration.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ApiResponse<T> {

    private static final String DEFAULT_MESSAGE = "요청이 성공적으로 처리되었습니다.";

    private final boolean success;
    private final T data;
    private final String message;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, DEFAULT_MESSAGE);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, DEFAULT_MESSAGE);
    }
}
