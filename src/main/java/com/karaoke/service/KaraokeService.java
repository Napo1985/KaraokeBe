package com.karaoke.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.karaoke.model.dto.GenerateKaraokeRequest;
import com.karaoke.model.dto.JobStatus;
import com.karaoke.model.entity.KaraokeJob;
import com.karaoke.repository.KaraokeJobRepository;
import com.karaoke.service.audio.AudioSeparationService;
import com.karaoke.service.lyrics.LyricsProvider;
import com.karaoke.service.lyrics.LyricsService;
import com.karaoke.service.video.KaraokeVideoGeneratorService;
import com.karaoke.service.video.VideoDownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class KaraokeService {

    private final KaraokeJobRepository jobRepository;
    private final VideoDownloadService videoDownloadService;
    private final AudioSeparationService audioSeparationService;
    private final LyricsService lyricsService;
    private final KaraokeVideoGeneratorService karaokeVideoGeneratorService;
    private final ObjectMapper objectMapper;

    @Transactional
    public KaraokeJob createJob(GenerateKaraokeRequest request) {
        KaraokeJob job = KaraokeJob.builder()
                .status(JobStatus.PENDING)
                .youtubeUrl(request.getYoutubeUrl())
                .progress(0)
                .options(serializeOptions(request))
                .build();
        
        return jobRepository.save(job);
    }

    @Async("karaokeTaskExecutor")
    @Transactional
    public void processJob(Long jobId) {
        KaraokeJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        
        try {
            updateJobStatus(job, JobStatus.PROCESSING, 0, null);
            
            GenerateKaraokeRequest request = deserializeOptions(job.getOptions());
            String jobIdStr = job.getId().toString();
            
            // Step 1: Download video (10% progress)
            log.info("Step 1: Downloading video for job {}", jobId);
            VideoDownloadService.VideoDownloadResult downloadResult = 
                    videoDownloadService.downloadVideo(job.getYoutubeUrl(), jobIdStr);
            updateJobProgress(job, 10);
            
            // Step 2: Separate audio (30% progress)
            log.info("Step 2: Separating audio for job {}", jobId);
            AudioSeparationService.AudioSeparationResult separationResult = 
                    audioSeparationService.separateAudio(downloadResult.getVideoPath(), downloadResult.getJobDir());
            updateJobProgress(job, 30);
            
            // Step 3: Get lyrics (50% progress)
            log.info("Step 3: Getting lyrics for job {}", jobId);
            // Extract artist and title from YouTube URL (simplified - in production, use yt-dlp metadata)
            String artist = "Unknown";
            String title = "Unknown";
            LyricsProvider.LyricsResult lyrics = lyricsService.getLyrics(
                    artist, title, separationResult.getVocalsPath());
            updateJobProgress(job, 50);
            
            // Step 4: Generate karaoke video (50-90% progress)
            log.info("Step 4: Generating karaoke video for job {}", jobId);
            String outputPath = karaokeVideoGeneratorService.generateKaraokeVideo(
                    downloadResult.getVideoPath(),
                    separationResult.getInstrumentalPath(),
                    separationResult.getVocalsPath(),
                    lyrics,
                    request.getIncludeBackgroundVocals(),
                    request.getVocalsVolume(),
                    jobIdStr
            );
            updateJobProgress(job, 90);
            
            // Step 5: Complete (100% progress)
            updateJobStatus(job, JobStatus.COMPLETED, 100, outputPath);
            log.info("Job {} completed successfully", jobId);
            
        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage(), e);
            updateJobStatus(job, JobStatus.FAILED, job.getProgress(), null);
            job.setErrorMessage(e.getMessage());
            jobRepository.save(job);
        }
    }

    @Transactional
    public KaraokeJob getJob(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found: " + id));
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<KaraokeJob> getAllJobs(org.springframework.data.domain.Pageable pageable) {
        return jobRepository.findAll(pageable);
    }

    private void updateJobStatus(KaraokeJob job, JobStatus status, int progress, String outputPath) {
        job.setStatus(status);
        job.setProgress(progress);
        if (outputPath != null) {
            job.setOutputVideoPath(outputPath);
        }
        jobRepository.save(job);
    }

    private void updateJobProgress(KaraokeJob job, int progress) {
        job.setProgress(progress);
        jobRepository.save(job);
    }

    private String serializeOptions(GenerateKaraokeRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            log.error("Failed to serialize options", e);
            return "{}";
        }
    }

    private GenerateKaraokeRequest deserializeOptions(String options) {
        try {
            return objectMapper.readValue(options, GenerateKaraokeRequest.class);
        } catch (Exception e) {
            log.error("Failed to deserialize options", e);
            return new GenerateKaraokeRequest();
        }
    }
}
