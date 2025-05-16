package com.shelf_sense_backend.controller;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.shelf_sense_backend.service.ReceiptOcrService;

@RestController
@RequestMapping("/api/receipts")
@CrossOrigin(origins = "http://localhost:5173")
public class ReceiptOcrController {

    private static final Logger logger = LoggerFactory.getLogger(ReceiptOcrController.class);
    
    @Autowired
    private ReceiptOcrService receiptOcrService;
    
    @PostMapping(value = "/extract-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> extractTextFromReceipt(@RequestParam("file") MultipartFile file) {
        logger.info("Received receipt text extraction request, filename: {}", file.getOriginalFilename());
    
        if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            return ResponseEntity.badRequest().body("Please upload a valid image file");
        }
        
        try {

            receiptOcrService.processReceipt(file);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            logger.info("Text extracted successfully from receipt image");
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            logger.error("Error extracting text from receipt image", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to extract text: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
} 
