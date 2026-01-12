package com.karaoke.service.video;

import com.karaoke.config.FileStorageConfig;
import com.karaoke.service.lyrics.LyricsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KaraokeVideoGeneratorService {

    private final FileStorageConfig fileStorageConfig;

    @Value("${app.tools.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    public String generateKaraokeVideo(
            String originalVideoPath,
            String instrumentalPath,
            String vocalsPath,
            LyricsProvider.LyricsResult lyrics,
            boolean includeBackgroundVocals,
            double vocalsVolume,
            String jobId) throws IOException, InterruptedException {
        
        log.info("Generating karaoke video for job: {}", jobId);
        
        String outputDir = fileStorageConfig.getOutputDirPath();
        String outputPath = Paths.get(outputDir, jobId + "_karaoke.mp4").toString();
        
        // Create filter complex for audio mixing
        List<String> ffmpegArgs = new ArrayList<>();
        ffmpegArgs.add(ffmpegPath);
        ffmpegArgs.add("-i");
        ffmpegArgs.add(originalVideoPath);
        ffmpegArgs.add("-i");
        ffmpegArgs.add(instrumentalPath);
        
        if (includeBackgroundVocals) {
            ffmpegArgs.add("-i");
            ffmpegArgs.add(vocalsPath);
        }
        
        // Video filter: extract video stream
        ffmpegArgs.add("-map");
        ffmpegArgs.add("0:v");
        
        // Audio filter: mix instrumental and optionally vocals
        if (includeBackgroundVocals) {
            // Mix instrumental and vocals with volume control
            String audioFilter = String.format(
                    "[1:a]volume=1.0[a1];[2:a]volume=%.2f[a2];[a1][a2]amix=inputs=2:duration=first:dropout_transition=2",
                    vocalsVolume);
            ffmpegArgs.add("-filter_complex");
            ffmpegArgs.add(audioFilter);
            ffmpegArgs.add("-map");
            ffmpegArgs.add("[a1]");
        } else {
            // Use only instrumental
            ffmpegArgs.add("-map");
            ffmpegArgs.add("1:a");
        }
        
        // Add lyrics as subtitles (simplified - bouncing ball would need more complex filter)
        if (lyrics != null && !lyrics.getLines().isEmpty()) {
            String subtitleFile = createSubtitleFile(lyrics, jobId);
            ffmpegArgs.add("-vf");
            ffmpegArgs.add(String.format("subtitles=%s:force_style='FontSize=24,PrimaryColour=&Hffffff,OutlineColour=&H000000,BorderStyle=1'", 
                    subtitleFile.replace("\\", "/").replace(":", "\\:")));
        }
        
        ffmpegArgs.add("-c:v");
        ffmpegArgs.add("libx264");
        ffmpegArgs.add("-c:a");
        ffmpegArgs.add("aac");
        ffmpegArgs.add("-shortest");
        ffmpegArgs.add("-y");
        ffmpegArgs.add(outputPath);
        
        ProcessBuilder processBuilder = new ProcessBuilder(ffmpegArgs);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new IOException("FFmpeg failed with exit code: " + exitCode);
        }
        
        log.info("Karaoke video generated successfully: {}", outputPath);
        return outputPath;
    }

    private String createSubtitleFile(LyricsProvider.LyricsResult lyrics, String jobId) throws IOException {
        String tempDir = fileStorageConfig.getTempDirPath();
        String subtitlePath = Paths.get(tempDir, jobId, "subtitles.srt").toString();
        
        List<String> srtLines = new ArrayList<>();
        int index = 1;
        
        for (LyricsProvider.LyricLine line : lyrics.getLines()) {
            srtLines.add(String.valueOf(index++));
            
            if (line.getStartTime() != null && line.getEndTime() != null) {
                String startTime = formatSrtTime(line.getStartTime());
                String endTime = formatSrtTime(line.getEndTime());
                srtLines.add(startTime + " --> " + endTime);
            } else {
                // Estimate timing if not available (rough estimate: 3 seconds per line)
                double estimatedStart = (index - 2) * 3.0;
                double estimatedEnd = estimatedStart + 3.0;
                srtLines.add(formatSrtTime(estimatedStart) + " --> " + formatSrtTime(estimatedEnd));
            }
            
            srtLines.add(line.getText());
            srtLines.add(""); // Empty line between entries
        }
        
        Files.write(Paths.get(subtitlePath), srtLines);
        return subtitlePath;
    }

    private String formatSrtTime(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int millis = (int) ((seconds % 1) * 1000);
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, secs, millis);
    }
}
