package com.karaoke.service;

import com.karaoke.config.FileStorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileCleanupService {

    private final FileStorageConfig fileStorageConfig;

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupOldFiles() {
        log.info("Starting file cleanup task");
        
        int retentionHours = fileStorageConfig.getRetentionHours();
        Instant cutoffTime = Instant.now().minus(retentionHours, ChronoUnit.HOURS);
        
        cleanupDirectory(fileStorageConfig.getTempDirPath(), cutoffTime);
        cleanupDirectory(fileStorageConfig.getOutputDirPath(), cutoffTime);
        
        log.info("File cleanup task completed");
    }

    private void cleanupDirectory(String directoryPath, Instant cutoffTime) {
        try {
            Path dir = Paths.get(directoryPath);
            if (!Files.exists(dir)) {
                return;
            }
            
            Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            FileTime fileTime = Files.getLastModifiedTime(path);
                            if (fileTime.toInstant().isBefore(cutoffTime)) {
                                Files.delete(path);
                                log.debug("Deleted old file: {}", path);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to delete file {}: {}", path, e.getMessage());
                        }
                    });
            
            // Clean up empty directories
            Files.walk(dir)
                    .filter(Files::isDirectory)
                    .sorted((a, b) -> b.compareTo(a)) // Process deepest first
                    .forEach(path -> {
                        try {
                            if (path.toFile().listFiles() == null || path.toFile().listFiles().length == 0) {
                                if (!path.equals(dir)) {
                                    Files.delete(path);
                                    log.debug("Deleted empty directory: {}", path);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed to delete directory {}: {}", path, e.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error during cleanup of directory {}: {}", directoryPath, e.getMessage());
        }
    }
}
