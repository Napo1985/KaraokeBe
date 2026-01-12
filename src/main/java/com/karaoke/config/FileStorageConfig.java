package com.karaoke.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Getter
public class FileStorageConfig {

    @Value("${app.storage.temp-dir:./temp}")
    private String tempDir;

    @Value("${app.storage.output-dir:./output}")
    private String outputDir;

    @Value("${app.storage.retention-hours:24}")
    private Integer retentionHours;

    @PostConstruct
    public void init() {
        try {
            Path tempPath = Paths.get(tempDir);
            Path outputPath = Paths.get(outputDir);
            
            if (!Files.exists(tempPath)) {
                Files.createDirectories(tempPath);
            }
            if (!Files.exists(outputPath)) {
                Files.createDirectories(outputPath);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create storage directories", e);
        }
    }

    public String getTempDirPath() {
        return new File(tempDir).getAbsolutePath();
    }

    public String getOutputDirPath() {
        return new File(outputDir).getAbsolutePath();
    }
}
