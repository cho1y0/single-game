package com.mixmaster.api.response;

import com.google.gson.Gson;

/**
 * 클라이언트(Unity)와의 통신을 위한 공통 JSON 응답 규격 (Wrapper Pattern)
 * 기획서 [4. 확장성 및 유지보수 고려사항] 준수
 */
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    // JSON 변환을 위한 Gson 인스턴스 (pom.xml에 추가됨)
    private static final Gson gson = new Gson();

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 1. 성공 응답 헬퍼 메서드 (데이터 포함)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "Success", data);
    }

    // 2. 성공 응답 헬퍼 메서드 (데이터 없음)
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(200, "Success", null);
    }

    // 3. 실패/에러 응답 헬퍼 메서드
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // 4. 객체를 JSON 문자열로 즉시 변환 (Servlet에서 출력 시 편리함)
    public String toJson() {
        return gson.toJson(this);
    }

    // Getters
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    
    // Setters
    public void setCode(int code) { this.code = code; }
    public void setMessage(String message) { this.message = message; }
    public void setData(T data) { this.data = data; }
}
