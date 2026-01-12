package com.karaoke.service.audio;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@Slf4j
public class AudioSeparationService {

    @Value("${app.tools.python-path:python}")
    private String pythonPath;

    public AudioSeparationResult separateAudio(String videoPath, String jobDir) throws IOException, InterruptedException {
        log.info("Separating audio from video: {}", videoPath);
        
        String scriptPath = Paths.get("src", "main", "resources", "scripts", "separate_audio.py").toString();
        File scriptFile = new File(scriptPath);
        
        if (!scriptFile.exists()) {
            // Try alternative path
            scriptPath = Paths.get("scripts", "separate_audio.py").toString();
            scriptFile = new File(scriptPath);
        }
        
        if (!scriptFile.exists()) {
            throw new IOException("Audio separation script not found at: " + scriptPath);
        }
        
        String outputDir = Paths.get(jobDir, "separated").toString();
        Files.createDirectories(Paths.get(outputDir));
        
        ProcessBuilder processBuilder = new ProcessBuilder(
                pythonPath,
                scriptFile.getAbsolutePath(),
                "--input", videoPath,
                "--output", outputDir
        );
        
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new IOException("Audio separation failed with exit code: " + exitCode);
        }
        
        String vocalsPath = Paths.get(outputDir, "vocals.wav").toString();
        String instrumentalPath = Paths.get(outputDir, "instrumental.wav").toString();
        
        if (!Files.exists(Paths.get(vocalsPath)) || !Files.exists(Paths.get(instrumentalPath))) {
            throw new IOException("Separated audio files not found");
        }
        
        log.info("Audio separated successfully. Vocals: {}, Instrumental: {}", vocalsPath, instrumentalPath);
        
        return new AudioSeparationResult(vocalsPath, instrumentalPath);
    }

    public static class AudioSeparationResult {
        private final String vocalsPath;
        private final String instrumentalPath;

        public AudioSeparationResult(String vocalsPath, String instrumentalPath) {
            this.vocalsPath = vocalsPath;
            this.instrumentalPath = instrumentalPath;
        }

        public String getVocalsPath() {
            return vocalsPath;
        }

        public String getInstrumentalPath() {
            return instrumentalPath;
        }
    }
}
