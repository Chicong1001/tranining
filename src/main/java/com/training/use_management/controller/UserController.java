package com.training.use_management.controller;

import com.training.use_management.dto.requestDTO.RegisterRequest;
import com.training.use_management.service.UserService;
import com.training.use_management.utils.ValidationUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import com.training.use_management.dto.responseDTO.UserProfileDTO;
import com.training.use_management.entity.User;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "5") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return userService.getUsers(page, size);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody RegisterRequest registerRequest,
                                        BindingResult result) {
        if (result.hasErrors()) {
            return ValidationUtil.handleValidationErrors(result);
        }
        return userService.createUser(registerRequest);
    }

    @PutMapping("{id}")
    public ResponseEntity<?> updateUser(@Valid @PathVariable Long id,
                                        @RequestBody User userDetails,
                                        BindingResult result) {
        if (result.hasErrors()) {
            return ValidationUtil.handleValidationErrors(result);
        }
        return userService.updateUser(id, userDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@Valid @PathVariable Long id) {
        return userService.deleteUser(id);
    }

    @PostMapping("/generate-fake-users")
    public ResponseEntity<?> generateFakeUsers(@RequestParam(defaultValue = "1000") int count) {
        String result = userService.generateFakeUsers(count);
        return ResponseEntity.ok(result);
    }
}

//    @PostMapping("/import")
//    public ResponseEntity<String> importCSV(@RequestParam("file") MultipartFile file) {
//        // Xử lý đọc file CSV và lưu vào database
//        return ResponseEntity.ok("Import thành công");
//    }
//
//    @GetMapping("/export")
//    public void exportCSV(HttpServletResponse response) throws IOException {
//        response.setContentType("text/csv");
//        response.setHeader("Content-Disposition", "attachment; filename=users.csv");
//        PrintWriter writer = response.getWriter();
//        writer.println("ID,Username,Email");
//        userRepository.findAll().forEach(user ->
//                writer.println(user.getId() + "," + user.getUsername() + "," + user.getEmail())
//        );
//    }

//    @PostMapping("/upload")
//    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
//        String fileUrl = s3Service.uploadFile(file);
//        return ResponseEntity.ok(fileUrl);
//    }
//
//    @GetMapping("/download/{filename}")
//    public ResponseEntity<byte[]> downloadFile(@PathVariable String filename) {
//        byte[] fileData = s3Service.downloadFile(filename);
//        return ResponseEntity.ok().body(fileData);
//    }

