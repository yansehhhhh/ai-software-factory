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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
public class ArtifactController {

    private final ClaudeRunnerConfig config;

    public ArtifactController(ClaudeRunnerConfig config) {
        this.config = config;
    }

    @GetMapping("/artifacts")
    public ResponseEntity<?> artifact(@RequestParam String path) throws Exception {
        Path artifactPath = resolveArtifactPath(path);
        if (!Files.exists(artifactPath) || !Files.isRegularFile(artifactPath)) {
            return ResponseEntity.notFound().build();
        }

        String fileName = artifactPath.getFileName().toString();
        if (fileName.toLowerCase().endsWith(".puml")) {
            return plantUmlPreview(artifactPath, fileName);
        }

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

    private ResponseEntity<String> plantUmlPreview(Path artifactPath, String fileName) throws Exception {
        String source = Files.readString(artifactPath, StandardCharsets.UTF_8);
        RenderedPlantUml rendered = renderPlantUmlSvg(artifactPath);
        String body = """
                <!doctype html>
                <html lang=\"zh-CN\">
                <head>
                  <meta charset=\"utf-8\" />
                  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                  <title>%s</title>
                  <style>
                    body { margin: 0; background: #f8fafc; color: #0f172a; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif; }
                    header { padding: 18px 24px; background: #fff; border-bottom: 1px solid #e2e8f0; }
                    h1 { margin: 0; font-size: 18px; }
                    main { padding: 24px; display: grid; gap: 18px; }
                    .panel { background: #fff; border: 1px solid #e2e8f0; border-radius: 14px; padding: 18px; box-shadow: 0 10px 28px rgba(15, 23, 42, 0.06); }
                    .diagram { overflow: auto; text-align: center; }
                    .diagram svg { max-width: 100%%; height: auto; }
                    .error { color: #b91c1c; background: #fef2f2; border-color: #fecaca; white-space: pre-wrap; }
                    pre { margin: 0; overflow: auto; line-height: 1.55; font-size: 13px; font-family: \"SFMono-Regular\", Consolas, monospace; }
                  </style>
                </head>
                <body>
                  <header><h1>%s</h1></header>
                  <main>%s<section class=\"panel\"><pre>%s</pre></section></main>
                </body>
                </html>
                """.formatted(
                escapeHtml(fileName),
                escapeHtml(fileName),
                rendered.svg() == null
                        ? "<section class=\"panel error\">PlantUML 渲染失败：" + escapeHtml(rendered.error()) + "</section>"
                        : "<section class=\"panel diagram\">" + rendered.svg() + "</section>",
                escapeHtml(source)
        );
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(fileName + ".html", StandardCharsets.UTF_8).build().toString())
                .body(body);
    }

    private RenderedPlantUml renderPlantUmlSvg(Path artifactPath) {
        List<String> command = plantUmlCommand();
        if (command.isEmpty()) {
            return new RenderedPlantUml(null, "未找到 plantuml 命令或 plantuml.jar");
        }
        command.add("-tsvg");
        command.add("-pipe");
        ProcessBuilder builder = new ProcessBuilder(command);
        String dotPath = findDotPath();
        if (dotPath != null) {
            builder.environment().put("GRAPHVIZ_DOT", dotPath);
        }
        try {
            Process process = builder.start();
            try (var output = process.getOutputStream()) {
                Files.copy(artifactPath, output);
            }
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            process.getInputStream().transferTo(stdout);
            process.getErrorStream().transferTo(stderr);
            int exitCode = process.waitFor();
            String svg = stdout.toString(StandardCharsets.UTF_8);
            if (exitCode == 0 && svg.contains("<svg")) {
                return new RenderedPlantUml(svg, null);
            }
            String error = stderr.toString(StandardCharsets.UTF_8);
            return new RenderedPlantUml(null, error.isBlank() ? "plantuml exited with code " + exitCode : error);
        } catch (Exception error) {
            return new RenderedPlantUml(null, error.getMessage());
        }
    }

    private List<String> plantUmlCommand() {
        String plantUmlPath = findExecutable("plantuml", List.of("/opt/homebrew/bin/plantuml", "/usr/local/bin/plantuml", "/opt/local/bin/plantuml"));
        if (plantUmlPath != null) {
            return new ArrayList<>(List.of(plantUmlPath));
        }
        String jarPath = findPlantUmlJar();
        if (jarPath != null) {
            return new ArrayList<>(List.of("java", "-jar", jarPath));
        }
        return new ArrayList<>();
    }

    private String findPlantUmlJar() {
        List<Path> roots = List.of(
                Path.of(System.getProperty("user.home"), ".vscode", "extensions"),
                Path.of(System.getProperty("user.home"), ".cursor", "extensions"),
                Path.of(System.getProperty("user.home"), ".vscode-insiders", "extensions")
        );
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root, 4)) {
                Optional<Path> jar = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().equalsIgnoreCase("plantuml.jar"))
                        .findFirst();
                if (jar.isPresent()) {
                    return jar.get().toString();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String findDotPath() {
        return findExecutable("dot", List.of("/opt/homebrew/bin/dot", "/usr/local/bin/dot", "/opt/local/bin/dot", "/usr/bin/dot"));
    }

    private String findExecutable(String command, List<String> candidates) {
        for (String candidate : candidates) {
            if (Files.isExecutable(Path.of(candidate))) {
                return candidate;
            }
        }
        try {
            Process process = new ProcessBuilder("/usr/bin/env", "which", command).start();
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            process.getInputStream().transferTo(stdout);
            if (process.waitFor() == 0) {
                String resolved = stdout.toString(StandardCharsets.UTF_8).trim();
                if (!resolved.isBlank() && Files.isExecutable(Path.of(resolved))) {
                    return resolved;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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

    private record RenderedPlantUml(String svg, String error) {
    }
}
