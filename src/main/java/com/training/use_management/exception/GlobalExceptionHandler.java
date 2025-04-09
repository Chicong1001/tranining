package com.training.use_management.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Xử lý lỗi User không tìm thấy
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    // Xử lý lỗi khi không thỏa mãn điều kiện validation
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
//        BindingResult result = ;
//
//        Map<String, String> fieldErrors = new HashMap<>();
//        for (FieldError error : result.getFieldErrors()) {
//            fieldErrors.put(error.getField(), error.getDefaultMessage());
//        }
//
//        Map<String, Object> response = new LinkedHashMap<>();
//        response.put("timestamp", System.currentTimeMillis());
//        response.put("status", HttpStatus.BAD_REQUEST.value());
//        response.put("errors", fieldErrors);
//
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        // Lấy BindingResult từ exception
        BindingResult result = ex.getBindingResult();

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : result.getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        // Tạo response chứa các thông tin lỗi
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("errors", fieldErrors);

        // Trả về ResponseEntity với lỗi 400
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Các ngoại lệ khác có thể bổ sung thêm ở đây
}


