package com.training.use_management.service;

import com.training.use_management.dto.requestDTO.LoginRequest;
import com.training.use_management.dto.requestDTO.UserRequest;
import com.training.use_management.dto.responseDTO.UserProfileDTO;
import com.training.use_management.entity.Role;
import com.training.use_management.entity.User;
import com.training.use_management.repository.RoleRepository;
import com.training.use_management.repository.UserRepository;
import com.training.use_management.security.JwtUtil;
import com.training.use_management.utils.PasswordUtil;
import com.training.use_management.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuthService {

    @Autowired
    private UserUtil userUtil;

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthService(RoleRepository roleRepository, UserRepository userRepository, JwtUtil jwtUtil) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public ResponseEntity<?> register(UserRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role not found: USER"));

        User user = UserUtil.createUser(request, Set.of(userRole));

        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully with username: " + user.getUsername());
    }

    // ✅  đăng nhập
    public ResponseEntity<?> login(LoginRequest request) {
        Optional<User> loginUsername = userRepository.findByUsername(request.getUsername());

        if (loginUsername.isPresent() && PasswordUtil.matches(request.getPassword(), loginUsername.get().getPassword())) {
            String token = jwtUtil.generateToken(loginUsername.get().getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", loginUsername.get().getUsername());
            response.put("roles", loginUsername.get().getRoles());

            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    // ✅ Lấy thông tin user từ JWT
    public ResponseEntity<?> getUserProfile(String username) {
        Optional<User> loginUsername = userRepository.findByUsername(username);

        // Nếu tìm thấy người dùng, trả về thông tin người dùng
        return loginUsername.map(user -> ResponseEntity.ok(new UserProfileDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRoles(),
                        "Password is correct with registered password"
                )))
                // Nếu không tìm thấy, trả về thông báo người dùng không tồn tại
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        new UserProfileDTO(null, "User not found", "", null, "")
                ));
    }

}