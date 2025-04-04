package com.training.use_management.utils;

import com.training.use_management.dto.requestDTO.UserRequest;
import com.training.use_management.entity.Role;
import com.training.use_management.entity.User;
import org.springframework.stereotype.Service;


import java.util.Set;

public class UserUtil {

    public static User createUser(UserRequest userRequest, Set<Role> roles) {
        return User.builder()
                .username(userRequest.getUsername())
                .email(userRequest.getEmail())
                .password(PasswordUtil.encodePassword(userRequest.getPassword().trim()))
                .roles(roles)
                .build();

    }
}