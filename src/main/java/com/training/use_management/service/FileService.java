package com.training.use_management.service;

import com.training.use_management.entity.Role;
import com.training.use_management.entity.User;
import com.training.use_management.repository.RoleRepository;
import com.training.use_management.repository.UserRepository;
import com.training.use_management.utils.FileUtil;
import com.training.use_management.utils.PasswordUtil;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FileUtil fileUtil;

    public FileService(UserRepository userRepository, RoleRepository roleRepository, FileUtil fileUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.fileUtil = fileUtil;
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
            // Kiểm tra kích thước file
            if (!fileUtil.isFileSizeValid(file.getSize())) {
                response.put("message", "File is too large. Max allowed size is " + fileUtil.getMaxFileSizeConfig());
                return response;
            }
            // Kiểm tra phần mở rộng file hợp lệ
            String originalFilename = file.getOriginalFilename();
            if (!fileUtil.isFileExtensionValid(originalFilename)) {
                response.put("message", "Invalid file type. Allowed types: csv");
                return response;
            }
            // Lưu tệp
            Path filePath = fileUtil.saveFile(file);

            response.put("message", "File uploaded successfully");
            response.put("filename", filePath.getFileName().toString());
        } catch (IOException e) {
            response.put("message", "Upload failed: " + e.getMessage());
        }
        return response;
    }

    // Download file
    public ResponseEntity<?> downloadFile(String filename) {
        try {
            // Đảm bảo thư mục tải lên là thư mục tuyệt đối
            Path path = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Path filePath = path.resolve(filename).normalize();

            // Kiểm tra nếu filePath không bắt đầu với thư mục tải lên, tránh xâm nhập
            if (!filePath.startsWith(path)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied!"));
            }

            // Kiểm tra sự tồn tại của tệp
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "File not found: " + filename));
            }

            // Xác định loại nội dung
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";  // Loại tệp mặc định
            }

            // Mã hóa tên tệp để xử lý tên có dấu cách hoặc ký tự đặc biệt
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

            // Tạo resource từ đường dẫn tệp
            Resource resource = new UrlResource(filePath.toUri());

            // Trả về ResponseEntity với thông tin tệp
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error processing file"));
        }
    }


    // Import file CSV và lưu dữ liệu vào cơ sở dữ liệu
    public String uploadAndImportFile(MultipartFile file) {
        if (!FileUtil.isValidCSV(file)) {
            return "Invalid file format. Please upload a valid CSV file.";
        }

        try {
            List<Map<String, String>> records = FileUtil.readCsvAsMap(file);
            if (records.isEmpty()) {
                return "CSV file is empty.";
            }

            Set<String> emailsInCsv = extractEmails(records);
            Set<String> existingEmails = findExistingEmails(emailsInCsv);

            List<User> users = createUsersFromRecords(records, existingEmails);

            userRepository.saveAll(users);
            return "✅ File imported successfully. " + users.size() + " users added.";

        } catch (IOException e) {
            return "❌ File read error: " + e.getMessage();
        } catch (IllegalArgumentException e) {
            return "❌ Data error: " + e.getMessage();
        } catch (Exception e) {
            return "❌ Unexpected error: " + e.getMessage();
        }
    }

    private Set<String> extractEmails(List<Map<String, String>> records) {
        return records.stream()
                .map(record -> record.get("email"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<String> findExistingEmails(Set<String> emailsInCsv) {
        return userRepository.findAllByEmailIn(emailsInCsv)
                .stream()
                .map(User::getEmail)
                .collect(Collectors.toSet());
    }

    private List<User> createUsersFromRecords(List<Map<String, String>> records, Set<String> existingEmails) {
        List<User> users = new ArrayList<>();
        Map<String, Role> roleCache = new HashMap<>();

        for (Map<String, String> record : records) {
            // Validate and create user
            if (existingEmails.contains(record.get("email"))) {
                throw new IllegalArgumentException("Duplicate email: " + record.get("email"));
            }

            User user = new User();
            user.setUsername(record.get("username").trim());
            user.setEmail(record.get("email").trim());
            user.setPassword(PasswordUtil.encodePassword(record.get("password").trim()));
            user.setRoles(getRoles(record.get("roles"), roleCache));

            users.add(user);
        }

        return users;
    }

    private Set<Role> getRoles(String rolesRaw, Map<String, Role> roleCache) {
        Set<Role> roles = new HashSet<>();
        for (String roleName : rolesRaw.split(";")) {
            Role role = roleCache.computeIfAbsent(roleName.trim(), key -> roleRepository.findByName(key).orElseThrow());
            roles.add(role);
        }
        return roles;
    }




    // Export dữ liệu user sang file CSV
    public ResponseEntity<Resource> exportUsers() {
        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }

        try {
            // Gọi FileUtil để xử lý xuất CSV
            ByteArrayOutputStream outputStream = FileUtil.exportUsersToCSV(users);

            // Tạo resource từ outputStream
            ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());

            // Trả về tài nguyên cho người dùng tải về
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
