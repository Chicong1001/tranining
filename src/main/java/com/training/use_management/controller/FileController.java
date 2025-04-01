package com.training.use_management.controller;

import com.training.use_management.entity.Role;
import com.training.use_management.entity.User;
import com.training.use_management.repository.RoleRepository;
import com.training.use_management.repository.UserRepository;
import com.training.use_management.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public FileController(FileService fileService, UserRepository userRepository, RoleRepository roleRepository) {
        this.fileService = fileService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Path path = Paths.get(UPLOAD_DIR + file.getOriginalFilename());
            Files.write(path, file.getBytes());
            return ResponseEntity.ok("Files uploaded successfully " + file.getOriginalFilename());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed");
        }

    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PROBLEM_XML)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename= \"" + filename + "\" ")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/upload-import")
    public ResponseEntity<String> uploadAndImportFile(@RequestParam("file") MultipartFile file) {
        try {
            List<User> users = new ArrayList<>();
            BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            String line;

            // Bỏ qua dòng tiêu đề nếu có
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");

                User user = new User();
                user.setUsername(data[0]);
                user.setEmail(data[1]);
                user.setPassword(data[2]);

                // Xử lý danh sách roles
                String[] roleNames = data[3].split(";");
                Set<Role> roles = Arrays.stream(roleNames)
                        .map(roleName -> roleRepository.findByName(roleName.trim())
                                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                        .collect(Collectors.toSet());

                user.setRoles(roles);
                users.add(user);
            }

            userRepository.saveAll(users);
            return ResponseEntity.ok("File uploaded and data imported successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Import failed: " + e.getMessage());
        }
    }

    @GetMapping("/export-users")
    public ResponseEntity<Resource> exportUsers() {
        try {
            List<User> users = userRepository.findAll();
            String filePath = "exports/users.csv";
            FileWriter writer = new FileWriter(filePath);
            writer.append("Username,Email\n");
            for (User user : users) {
                writer.append(user.getUsername()).append(",").append(user.getEmail()).append("\n");
            }
            writer.close();

            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users.csv\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}
