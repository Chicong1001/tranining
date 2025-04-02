package com.training.use_management.controller;

import com.training.use_management.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
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

    //    @PostMapping("/upload")
//    public ResponseEntity<Map<String, String>>ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
//        return ResponseEntity.ok(fileService.uploadFile(file));
//    }
    @Operation(summary = "Upload a file")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        // Xử lý file upload
        return ResponseEntity.ok(fileService.uploadFile(file));
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<?> downloadFile(@PathVariable String filename) {
        return fileService.downloadFile(filename);
    }

    @PostMapping(value ="/upload-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadAndImportFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(fileService.uploadAndImportFile(file));
    }

    @GetMapping("/export-users")
    public ResponseEntity<?> exportUsers() {
        return fileService.exportUsers();
    }
}
