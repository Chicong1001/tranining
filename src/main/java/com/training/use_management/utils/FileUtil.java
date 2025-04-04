package com.training.use_management.utils;

import com.training.use_management.entity.Role;
import com.training.use_management.entity.User;
import com.training.use_management.repository.RoleRepository;
import com.training.use_management.repository.UserRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class FileUtil {

    @Value("${app.upload.max-file-size}")
    private String maxFileSizeConfig;

    private static final String UPLOAD_DIR = "uploads/";

    public enum FileSizeUnit {
        KB(1024), MB(1024 * 1024), GB(1024 * 1024 * 1024);

        private final long factor;

        FileSizeUnit(long factor) {
            this.factor = factor;
        }

        public long getFactor() {
            return factor;
        }

        public static long convertToBytes(String size) {
            size = size.toUpperCase();
            for (FileSizeUnit unit : values()) {
                if (size.endsWith(unit.name())) {
                    return Long.parseLong(size.replace(unit.name(), "").trim()) * unit.getFactor();
                }
            }
            return Long.parseLong(size.trim());  // Default to bytes
        }
    }

    // Chuyển đổi dung lượng sang byte
    public long getMaxFileSizeInBytes() {
        return FileSizeUnit.convertToBytes(maxFileSizeConfig);
    }

    public boolean isFileSizeValid(long fileSize) {
        return fileSize <= getMaxFileSizeInBytes();
    }

    public String getMaxFileSizeConfig() {
        return maxFileSizeConfig;
    }

    public static List<Map<String, String>> readCsvAsMap(MultipartFile file) throws IOException {
        List<Map<String, String>> records = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                Map<String, String> row = new HashMap<>();
                for (String header : record.toMap().keySet()) {
                    row.put(header, record.get(header).trim());
                }
                records.add(row);
            }
        }

        return records;
    }

    public static boolean isValidCSV(MultipartFile file) {
        return !file.isEmpty() && Objects.requireNonNull(file.getContentType()).equals("text/csv");
    }

    public static ByteArrayOutputStream exportUsersToCSV(List<User> users) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("Username", "Email", "Roles"))) {

            // Thêm BOM cho UTF-8
            outputStream.write(0xEF);
            outputStream.write(0xBB);
            outputStream.write(0xBF);

            // Xuất dữ liệu người dùng ra CSV
            for (User user : users) {
                String roles = user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.joining(";"));
                csvPrinter.printRecord(user.getUsername(), user.getEmail(), roles);
            }

            csvPrinter.flush(); // Đảm bảo dữ liệu được ghi ra
        }
        return outputStream;
    }

    public boolean isFileExtensionValid(String filename) {
        List<String> allowedExtensions = List.of("csv");
        String extension = FilenameUtils.getExtension(filename).toLowerCase();
        return allowedExtensions.contains(extension);
    }

    public Path saveFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String safeFilename = UUID.randomUUID().toString() + "_" + Paths.get(originalFilename).getFileName().toString();
        Path uploadDir = Paths.get(UPLOAD_DIR);

        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        Path path = uploadDir.resolve(safeFilename);

        // Kiểm tra trùng lặp tên tệp
        int counter = 1;
        while (Files.exists(path)) {
            safeFilename = UUID.randomUUID().toString() + "_" + counter++ + "_" + Paths.get(originalFilename).getFileName().toString();
            path = uploadDir.resolve(safeFilename);
        }

        try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(path))) {
            bos.write(file.getBytes());
        }

        return path;
    }
}