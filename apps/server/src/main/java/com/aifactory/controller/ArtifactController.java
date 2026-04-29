package com.aifactory.controller;

import com.aifactory.config.ClaudeRunnerConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api")
public class ArtifactController {

    private final ClaudeRunnerConfig config;

    public ArtifactController(ClaudeRunnerConfig config) {
        this.config = config;
    }

    @GetMapping("/artifacts")
    public ResponseEntity<Resource> artifact(@RequestParam String path) throws Exception {
        Path artifactPath = resolveArtifactPath(path);
        if (!Files.exists(artifactPath) || !Files.isRegularFile(artifactPath)) {
            return ResponseEntity.notFound().build();
        }

        String fileName = artifactPath.getFileName().toString();
        MediaType mediaType = mediaType(fileName);
        ContentDisposition disposition = inline(fileName)
                ? ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8).build()
                : ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(artifactPath));
    }

    private Path resolveArtifactPath(String artifactPath) {
        Path path = Path.of(artifactPath).normalize();
        if (path.isAbsolute()) {
            return path;
        }

        Path serverRelative = path.toAbsolutePath().normalize();
        if (Files.exists(serverRelative)) {
            return serverRelative;
        }

        Path repoRelative = Path.of("..").resolve("..").resolve(artifactPath).toAbsolutePath().normalize();
        if (Files.exists(repoRelative)) {
            return repoRelative;
        }

        return Path.of("..").resolve("claude-runner").resolve(artifactPath).toAbsolutePath().normalize();
    }

    private MediaType mediaType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".html")) {
            return MediaType.TEXT_HTML;
        }
        if (lower.endsWith(".svg")) {
            return MediaType.parseMediaType("image/svg+xml");
        }
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".md") || lower.endsWith(".txt") || lower.endsWith(".puml")) {
            return MediaType.parseMediaType("text/plain;charset=UTF-8");
        }
        if (lower.endsWith(".docx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        if (lower.endsWith(".zip")) {
            return MediaType.parseMediaType("application/zip");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private boolean inline(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".html") || lower.endsWith(".md") || lower.endsWith(".txt") || lower.endsWith(".puml") || lower.endsWith(".svg") || lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }
}
