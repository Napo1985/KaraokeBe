package com.karaoke.service.video;

import com.karaoke.config.FileStorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoDownloadService {

    private final FileStorageConfig fileStorageConfig;

    @Value("${app.tools.yt-dlp-path:yt-dlp}")
    private String ytDlpPath;

    public VideoDownloadResult downloadVideo(String youtubeUrl, String jobId) throws IOException, InterruptedException {
        log.info("Downloading video from URL: {}", youtubeUrl);
        
        String tempDir = fileStorageConfig.getTempDirPath();
        String jobDir = Paths.get(tempDir, jobId).toString();
        Files.createDirectories(Paths.get(jobDir));
        
        String outputPath = Paths.get(jobDir, "video.%(ext)s").toString();
        
        ProcessBuilder processBuilder = new ProcessBuilder(
                ytDlpPath,
                "-x", // Extract audio only
                "--audio-format", "wav",
                "--audio-quality", "0", // Best quality
                "-o", outputPath,
                youtubeUrl
        );
        
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new IOException("yt-dlp failed with exit code: " + exitCode);
        }
        
        // Find the downloaded file
        File jobDirFile = new File(jobDir);
        File[] files = jobDirFile.listFiles((dir, name) -> name.startsWith("video."));
        
        if (files == null || files.length == 0) {
            throw new IOException("Downloaded video file not found");
        }
        
        File videoFile = files[0];
        String videoPath = videoFile.getAbsolutePath();
        
        log.info("Video downloaded successfully to: {}", videoPath);
        
        return new VideoDownloadResult(videoPath, jobDir);
    }

    public static class VideoDownloadResult {
        private final String videoPath;
        private final String jobDir;

        public VideoDownloadResult(String videoPath, String jobDir) {
            this.videoPath = videoPath;
            this.jobDir = jobDir;
        }

        public String getVideoPath() {
            return videoPath;
        }

        public String getJobDir() {
            return jobDir;
        }
    }
}
