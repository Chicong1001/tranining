package com.training.use_management.controller;

import com.training.use_management.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileService.uploadFile(file));
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<?> downloadFile(@PathVariable String filename) {
        return fileService.downloadFile(filename);
    }

    @PostMapping("/upload-import")
    public ResponseEntity<String> uploadAndImportFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileService.uploadAndImportFile(file));
    }

    @GetMapping("/export-users")
    public ResponseEntity<?> exportUsers() {
        return fileService.exportUsers();
    }
}
