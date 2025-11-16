package com.ft.searchengine.controller;



import com.ft.searchengine.service.DataImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@RestController
@RequestMapping("/api/import")
@Slf4j
public class DataImportController {

    @Autowired
    private DataImportService dataImportService;

    @PostMapping("/csv")
    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(".csv")) {
            return ResponseEntity.badRequest().body("File must be a CSV");
        }

        log.info("Importing file: {}", file.getOriginalFilename());

        DataImportService.ImportResult result = dataImportService.importFromCsv(file);

        return ResponseEntity.ok(result);
    }
}