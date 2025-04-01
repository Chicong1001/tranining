package com.training.use_management.controller;

import com.training.use_management.dto.requestDTO.LoginRequest;
import com.training.use_management.dto.requestDTO.RegisterRequest;
import com.training.use_management.service.AuthService;
import com.training.use_management.utils.ValidationUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ✅ API Đăng ký
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request,
                                      BindingResult result) {
        if (result.hasErrors()) {
            return ValidationUtil.handleValidationErrors(result);
        }
        return authService.register(request);
    }

    // ✅ API Đăng nhập
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   BindingResult result) {
        if (result.hasErrors()) {
            return ValidationUtil.handleValidationErrors(result);
        }
        return authService.login(request);
    }

    // ✅ API Lấy thông tin user từ JWT
    @GetMapping("/profileWithJWT")
    public ResponseEntity<?> getUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return authService.getUserProfile(userDetails.getUsername());
    }
}