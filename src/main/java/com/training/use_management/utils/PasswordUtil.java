package com.training.use_management.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public static String encodePassword(String rawPassword){
        return passwordEncoder.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String encodePassword){
        return passwordEncoder.matches(rawPassword, encodePassword);
    }
}
