package com.aifactory.controller;

import com.aifactory.dto.GeneratedProjectPreviewRequest;
import com.aifactory.dto.GeneratedProjectPreviewResponse;
import com.aifactory.service.GeneratedProjectPreviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/generated-projects")
public class GeneratedProjectPreviewController {

    private final GeneratedProjectPreviewService previewService;

    public GeneratedProjectPreviewController(GeneratedProjectPreviewService previewService) {
        this.previewService = previewService;
    }

    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestBody GeneratedProjectPreviewRequest request) {
        try {
            GeneratedProjectPreviewResponse response = previewService.preview(request.path(), request.projectName());
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
        }
    }
}
