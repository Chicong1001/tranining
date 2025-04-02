package com.training.use_management.service;

import com.training.use_management.entity.Role;
import com.training.use_management.entity.User;
import com.training.use_management.repository.RoleRepository;
import com.training.use_management.repository.UserRepository;
import com.training.use_management.utils.PasswordUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public FileService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    private static final String UPLOAD_DIR = "uploads/";

    // Upload file
    public Map<String, String> uploadFile(MultipartFile file) {
        Map<String, String> response = new HashMap<>();
        try {
            if (file.isEmpty()) {
                response.put("message", "File is empty");
                return response;
            }

            // Kiểm tra phần mở rộng file hợp lệ
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            List<String> allowedExtensions = Arrays.asList("csv");

            if (!allowedExtensions.contains(fileExtension.toLowerCase())) {
                response.put("message", "Invalid file type. Allowed types: csv");
                return response;
            }

            String safeFilename = UUID.randomUUID().toString() + "_" + Paths.get(originalFilename).getFileName().toString();
            Path uploadDir = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path path = uploadDir.resolve(safeFilename);
            try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(path))) {
                bos.write(file.getBytes());
            }

            response.put("message", "File uploaded successfully");
            response.put("filename", safeFilename);
        } catch (IOException e) {
            response.put("message", "Upload failed: " + e.getMessage());
        }
        return response;
    }

    // Download file
    public ResponseEntity<?> downloadFile(String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
            if (!filePath.startsWith(Paths.get(UPLOAD_DIR))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied!"));
            }

            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "File not found: " + filename));
            }

            String contenType = Files.probeContentType(filePath);
            if (contenType == null) {
                contenType = "application/octet-stream";
            }
            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contenType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename= \"" + filename + "\" ")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error processing file"));
        }
    }

    // Import file CSV và lưu dữ liệu vào cơ sở dữ liệu
    public String uploadAndImportFile(MultipartFile file) {
        if (file.isEmpty()) {
            return "Uploaded file is empty";
        }

        if (!Objects.requireNonNull(file.getContentType()).equals("text/csv")) {
            return "Invalid file type. Only CSV files are allowed.";
        }

        List<User> users = new ArrayList<>();
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                if (record.size() < 4) {
                    return "Invalid CSV format. Each row must have 4 columns.";
                }

                String email = record.get("email").trim();
                if (userRepository.existsByEmail(email)) {
                    return "Duplicate email found: " + email;
                }
                User user = new User();
                user.setUsername(record.get("username").trim());
                user.setEmail(email);
                user.setPassword(PasswordUtil.encodePassword(record.get("password").trim()));
                // Xử lý roles
                Set<Role> roles = new HashSet<>();
                String[] roleNames = record.get("roles").split(";");
                for (String roleName : roleNames) {
                    try {
                        Role role = roleRepository.findByName(roleName.trim())
                                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
                        roles.add(role);
                    } catch (IllegalArgumentException ex) {
                        return ex.getMessage();
                    }
                }

                user.setRoles(roles);
                users.add(user);
            }
            userRepository.saveAll(users);
            return "File uploaded and data imported successfully";
        } catch (IOException e) {
            return "Import failed: " + e.getMessage();
        }
    }

    // Export dữ liệu user sang file CSV
    public ResponseEntity<Resource> exportUsers() {
        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            outputStream.write(0xEF);
            outputStream.write(0xBB);
            outputStream.write(0xBF);

            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("Username", "Email", "Roles"));
            for (User user : users) {
                String roles = user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.joining(";"));
                csvPrinter.printRecord(user.getUsername(), user.getEmail(), roles);
            }
            csvPrinter.flush();
            csvPrinter.close();
            ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename= \"users.csv\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ByteArrayResource(("Error exporting users: " + e.getMessage()).getBytes()));
        }
    }
}
