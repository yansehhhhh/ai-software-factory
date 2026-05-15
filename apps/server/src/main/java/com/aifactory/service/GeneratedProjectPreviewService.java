package com.aifactory.service;

import com.aifactory.dto.GeneratedProjectPreviewResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class GeneratedProjectPreviewService {

    private final Map<Path, PreviewProcess> previews = new HashMap<>();
    private final Path repoRoot = resolveRepoRoot();
    private final Path generatedRoot = repoRoot.resolve("generated").normalize();

    public synchronized GeneratedProjectPreviewResponse preview(String projectPath, String projectName) throws IOException, InterruptedException {
        Path projectDir = resolveProjectDir(projectPath, projectName);
        stopAllPreviews();
        stopProjectProcesses(projectDir.getParent());
        installDependenciesIfMissing(projectDir);
        int backendPort = availablePort();
        Process backendProcess = startBackendIfAvailable(projectDir, backendPort);
        writeRuntimeConfig(projectDir, backendProcess == null ? "" : "http://127.0.0.1:" + backendPort + "/api/hx-meeting/v1");
        int port = availablePort();
        ProcessBuilder builder = new ProcessBuilder("npm", "run", "dev", "--", "--host", "127.0.0.1", "--port", String.valueOf(port));
        builder.environment().put("VITE_USE_DEMO", "false");
        builder.environment().put("VITE_API_TARGET", "http://127.0.0.1:" + backendPort);
        Process process = builder
                .directory(projectDir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(projectDir.resolve("preview.log").toFile())
                .start();
        previews.put(projectDir, new PreviewProcess(process, port, backendProcess, backendPort));
        waitUntilStarted(projectDir, process);
        return response("started", port, projectDir);
    }

    private void stopAllPreviews() {
        previews.values().forEach(this::stopPreview);
        previews.clear();
    }

    private void stopPreview(PreviewProcess preview) {
        stopProcess(preview.process());
        stopProcess(preview.backendProcess());
    }

    private void stopProcess(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    private void stopProjectProcesses(Path projectRoot) {
        String projectPath = projectRoot.toAbsolutePath().normalize().toString();
        ProcessHandle.allProcesses()
                .filter(process -> process.info().commandLine().stream().anyMatch(command -> command.contains(projectPath)))
                .filter(process -> process.pid() != ProcessHandle.current().pid())
                .forEach(this::stopProcessHandle);
    }

    private void stopProcessHandle(ProcessHandle process) {
        process.destroy();
        try {
            Optional<ProcessHandle> alive = process.onExit().get(5, TimeUnit.SECONDS).isAlive() ? Optional.of(process) : Optional.empty();
            alive.ifPresent(ProcessHandle::destroyForcibly);
        } catch (Exception exception) {
            process.destroyForcibly();
        }
    }

    private Path resolveRepoRoot() {
        Path current = Path.of(".").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve("CLAUDE.md")) && Files.isDirectory(current.resolve("generated"))) {
                return current;
            }
            current = current.getParent();
        }
        return Path.of(".").toAbsolutePath().normalize();
    }

    private Path resolveProjectDir(String projectPath, String projectName) throws IOException {
        Path resolved = null;
        if (projectPath != null && !projectPath.isBlank()) {
            Path rawPath = Path.of(projectPath).normalize();
            resolved = rawPath.isAbsolute() ? rawPath : repoRoot.resolve(rawPath).normalize();
        }

        Path frontendDir = resolved == null ? null : normalizeFrontendDir(resolved);
        if (frontendDir == null || !frontendDir.startsWith(generatedRoot) || !Files.exists(frontendDir.resolve("package.json"))) {
            frontendDir = fallbackFrontendDir(projectName);
        }
        if (frontendDir == null || !Files.exists(frontendDir.resolve("package.json"))) {
            throw new IllegalArgumentException("未找到可运行的前端项目 package.json。");
        }
        if (!frontendDir.startsWith(generatedRoot)) {
            throw new IllegalArgumentException("只能预览 generated 目录下的项目。");
        }
        return frontendDir.toRealPath().normalize();
    }

    private Path normalizeFrontendDir(Path projectDir) {
        if (!projectDir.startsWith(generatedRoot)) {
            return null;
        }
        return Files.isDirectory(projectDir.resolve("frontend")) ? projectDir.resolve("frontend") : projectDir;
    }

    private Path fallbackFrontendDir(String projectName) {
        if ("移动端应用：会议室预约系统".equals(projectName) || "HX-Meeting 会议室预约系统".equals(projectName) || "会议室预约系统".equals(projectName)) {
            return generatedRoot.resolve("HX-Meeting/frontend").normalize();
        }
        return null;
    }

    private void installDependenciesIfMissing(Path projectDir) throws IOException, InterruptedException {
        if (Files.isDirectory(projectDir.resolve("node_modules"))) {
            return;
        }
        Process process = new ProcessBuilder("npm", "install")
                .directory(projectDir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(projectDir.resolve("install.log").toFile())
                .start();
        boolean completed = process.waitFor(5, TimeUnit.MINUTES);
        if (!completed) {
            process.destroyForcibly();
            throw new IllegalStateException("安装依赖超时，请查看 install.log。");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("安装依赖失败，请查看 install.log。");
        }
    }

    private int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private void writeRuntimeConfig(Path frontendDir, String apiBaseUrl) throws IOException {
        Path publicDir = frontendDir.resolve("public");
        Files.createDirectories(publicDir);
        String escapedApiBaseUrl = apiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"");
        Files.writeString(publicDir.resolve("runtime-config.json"), "{\n  \"apiBaseUrl\": \"" + escapedApiBaseUrl + "\"\n}\n");
    }

    private Process startBackendIfAvailable(Path frontendDir, int port) throws IOException, InterruptedException {
        Path backendDir = frontendDir.getParent().resolve("backend").normalize();
        if (!Files.isRegularFile(backendDir.resolve("pom.xml"))) {
            return null;
        }
        ProcessBuilder builder = new ProcessBuilder("mvn", "spring-boot:run", "-Dspring-boot.run.arguments=--server.port=" + port);
        builder.environment().put("SERVER_PORT", String.valueOf(port));
        Process process = builder
                .directory(backendDir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(backendDir.resolve("preview-backend.log").toFile())
                .start();
        waitUntilBackendStarted(backendDir, process, port);
        return process;
    }

    private void waitUntilBackendStarted(Path backendDir, Process process, int port) throws InterruptedException, IOException {
        long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
        Path logFile = backendDir.resolve("preview-backend.log");
        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) {
                throw new IllegalStateException("生成项目后端启动失败，请查看 preview-backend.log。");
            }
            if (Files.exists(logFile)) {
                String log = Files.readString(logFile);
                if (log.contains("Tomcat started on port " + port) || log.contains("Started HxMeetingApplication")) {
                    return;
                }
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("生成项目后端启动超时，请查看 preview-backend.log。");
    }

    private void waitUntilStarted(Path projectDir, Process process) throws InterruptedException, IOException {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        Path logFile = projectDir.resolve("preview.log");
        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) {
                throw new IllegalStateException("预览服务启动失败，请查看 preview.log。");
            }
            if (Files.exists(logFile)) {
                String log = Files.readString(logFile);
                if (log.contains("Local:") || log.contains("ready in") || log.contains("http://127.0.0.1")) {
                    return;
                }
            }
            Thread.sleep(500);
        }
        throw new IllegalStateException("预览服务启动超时，请查看 preview.log。");
    }

    private GeneratedProjectPreviewResponse response(String status, int port, Path projectDir) {
        return new GeneratedProjectPreviewResponse(status, "http://127.0.0.1:" + port + "/", port, repoRoot.relativize(projectDir).toString());
    }

    private record PreviewProcess(Process process, int port, Process backendProcess, int backendPort) {
    }
}
