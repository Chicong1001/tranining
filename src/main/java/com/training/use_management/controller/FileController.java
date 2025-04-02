package com.training.use_management.controller;

import com.training.use_management.entity.Role;
import com.training.use_management.entity.User;
import com.training.use_management.repository.RoleRepository;
import com.training.use_management.repository.UserRepository;
import com.training.use_management.utils.PasswordUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.csv.CSVParser;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/files")
public class FileController {

    //private final FileService fileService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public FileController(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    private static final String UPLOAD_DIR = "uploads/";

    //    @PostMapping("/upload")
//    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
//        try {
//            Path path = Paths.get(UPLOAD_DIR + file.getOriginalFilename());
//            Files.write(path, file.getBytes());
//            return ResponseEntity.ok("Files uploaded successfully " + file.getOriginalFilename());
//        } catch (IOException e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed");
//        }
//
//    }
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();

        try {
            // Kiểm tra file rỗng
            if (file.isEmpty()) {
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Giới hạn kích thước file (10MB)
//            long maxFileSize = 10 * 1024 * 1024; // 10MB
//            if (file.getSize() > maxFileSize) {
//                response.put("message", "File is too large. Max allowed size is 10MB.");
//                return ResponseEntity.badRequest().body(response);
//            }

            // Kiểm tra phần mở rộng file hợp lệ
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            List<String> allowedExtensions = Arrays.asList("" +
//                    "jpg", "png", "pdf", "txt",
                    "csv");

            if (!allowedExtensions.contains(fileExtension.toLowerCase())) {
                response.put("message", "Invalid file type. Allowed types: " +
                        "" + "csv" +  //jpg, png, pdf, txt,
                        "" + "");
                return ResponseEntity.badRequest().body(response);
            }

            // Tránh Path Traversal Attack
            //String safeFilename = Paths.get(originalFilename).getFileName().toString();
            // Tránh hai người tải lên file cùng tên, file trước có thể bị ghi đè.
            String safeFilename = UUID.randomUUID().toString() + "_" + Paths.get(originalFilename).getFileName().toString();

            // Đảm bảo thư mục tồn tại
            Path uploadDir = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Lưu file vào thư mục an toàn
            Path path = uploadDir.resolve(safeFilename);
            //Files.write(path, file.getBytes());
            //Sử dụng BufferedOutputStream để tối ưu hiệu suất nếu file lớn
            try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(path))) {
                bos.write(file.getBytes());
            }

            response.put("message", "File uploaded successfully");
            response.put("filename", safeFilename);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<?> downloadFile(@PathVariable String filename) {
        try {
            //Path filePath = Paths.get(UPLOAD_DIR + filename);
            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
            // Kiểm tra tránh Path traversal Attack
            if (!filePath.startsWith(Paths.get(UPLOAD_DIR))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied!"));
            }
            // Kiểm tra fiel có tồn tại hay không
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "File not found: " + filename));
            }
            //Lấy MIME type chính
            String contenType = Files.probeContentType(filePath);
            if (contenType == null) {
                contenType = "application/octet-stream";
            }
            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contenType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename= \"" + filename + "\" ")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error processing file"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Could not determine file type"));
        }
    }

    @PostMapping("/upload-import")
    public ResponseEntity<String> uploadAndImportFile(@RequestParam("file") MultipartFile file) {
        //Kiểm tra file có rỗng hay không
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Uploaded file is empty");
        }
        // Kiểm tra file type
        if (!Objects.requireNonNull(file.getContentType()).equals("text/csv")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file type. Only CSV files are allowed.");
        }
        List<User> users = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                if (record.size() < 4) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid CSV format. Each row must have 4 columns.");
                }

                String email = record.get("email").trim();
                if (userRepository.existsByEmail(email)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Duplicate email found: " + email);
                }
                User user = new User();
                user.setUsername(record.get("username").trim());
                user.setEmail(email);
                user.setPassword(PasswordUtil.encodePassword(record.get("password").trim()));
                //Xử ký roles
                Set<Role> roles = new HashSet<>();
                String[] roleNames = record.get("roles").split(";");
                for (String roleName : roleNames) {
                    try {
                        Role role = roleRepository.findByName(roleName.trim())
                                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
                        roles.add(role);
                    } catch (IllegalArgumentException ex) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
                    }
                }

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
        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            //  thêm BOM UTF-8 để hỗ trợ tiếng Viêt
            outputStream.write(0xEF);
            outputStream.write(0xBB);
            outputStream.write(0xBF);

            //Ghi CSV
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("Username", "Email"));
            for (User user : users) {
                csvPrinter.printRecord(user.getUsername(), user.getEmail());
            }
            csvPrinter.flush();
            csvPrinter.close();
            // Chuyển thành ByteArrayResource
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
