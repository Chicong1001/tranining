package com.training.use_management.service;

import com.github.javafaker.Faker;
import com.training.use_management.dto.requestDTO.RegisterRequest;
import com.training.use_management.dto.responseDTO.UserProfileDTO;
import com.training.use_management.entity.Role;
import com.training.use_management.entity.User;
import com.training.use_management.repository.RoleRepository;
import com.training.use_management.repository.UserRepository;
import com.training.use_management.utils.PasswordUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final Faker faker = new Faker();

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public ResponseEntity<UserProfileDTO> getUserById(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        return userOptional
                .map(user -> {
                    UserProfileDTO userProfileDTO = new UserProfileDTO(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getRoles(),
                            "Password is correct with register password"
                    );
                    return ResponseEntity.ok(userProfileDTO);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    public ResponseEntity<Page<User>> getUsers(int page, int size) {
        if (page < 0 || size <= 0) {
            return ResponseEntity.badRequest().body(null);
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userRepository.findAll(pageable);
        if (users.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(users);
    }

    public ResponseEntity<?> createUser(RegisterRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        System.out.println(currentUsername);
        Optional<User> adminUserOpt = userRepository.findUserWithAdminRole(currentUsername);
        System.out.println(adminUserOpt);
        if (!adminUserOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Admin role required");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Email is already registered!");
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(PasswordUtil.encodePassword(request.getPassword()));

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Role not found: USER"));

        newUser.setRoles(Collections.singleton(userRole));
//        newUser.setRoles(Set.of(new Role("USER")));
        User createUser = userRepository.save(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(createUser);
    }

    public ResponseEntity<User> updateUser(Long id, User userDetails) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setUsername(userDetails.getUsername());
                    user.setEmail(userDetails.getEmail());
                    user.setPassword(PasswordUtil.encodePassword(userDetails.getPassword()));

                    Role userRole = roleRepository.findByName("USER")
                            .orElseThrow(() -> new RuntimeException("Role not found: USER"));

                    user.setRoles(Collections.singleton(userRole));

                    return ResponseEntity.ok(userRepository.save(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<String> deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: User with ID" + id + " not found");
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok("User with Id: " + id + " has been deleted successfully.");
    }

    public String generateFakeUsers(int count) {
        List<User> users = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            User user = new User();
            user.setUsername(faker.name().username());  // Tên ngẫu nhiên
            user.setEmail(faker.internet().emailAddress());  // Email ngẫu nhiên
            user.setPassword(PasswordUtil.encodePassword("Password123!")); // Mật khẩu cố định

            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Role not found: USER"));
            user.setRoles(Collections.singleton(userRole));

            users.add(user);
        }

        userRepository.saveAll(users);
        return "✅ " + count + " fake users have been created!";
    }

}
