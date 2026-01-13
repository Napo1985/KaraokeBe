package com.karaoke.controller;

import com.karaoke.model.dto.GenerateKaraokeRequest;
import com.karaoke.model.dto.KaraokeJobResponse;
import com.karaoke.model.entity.KaraokeJob;
import com.karaoke.service.KaraokeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping("/karaoke")
@RequiredArgsConstructor
@Tag(name = "Karaoke", description = "Karaoke video generation endpoints")
public class KaraokeController {

    private final KaraokeService karaokeService;

    @PostMapping("/generate")
    @Operation(summary = "Generate karaoke video", 
               description = "Creates a new karaoke video generation job from a YouTube URL. Returns job ID for status tracking.")
    public ResponseEntity<KaraokeJobResponse> generateKaraoke(@Valid @RequestBody GenerateKaraokeRequest request) {
        KaraokeJob job = karaokeService.createJob(request);
        karaokeService.processJob(job.getId());
        
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(mapToResponse(job));
    }

    @GetMapping("/jobs/{id}")
    @Operation(summary = "Get job status", 
               description = "Retrieves the status and progress of a karaoke generation job")
    public ResponseEntity<KaraokeJobResponse> getJobStatus(@PathVariable Long id) {
        KaraokeJob job = karaokeService.getJob(id);
        return ResponseEntity.ok(mapToResponse(job));
    }

    @GetMapping("/jobs/{id}/download")
    @Operation(summary = "Download karaoke video", 
               description = "Downloads the generated karaoke video file")
    public ResponseEntity<Resource> downloadVideo(@PathVariable Long id) {
        KaraokeJob job = karaokeService.getJob(id);
        
        if (job.getStatus() != com.karaoke.model.dto.JobStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        
        if (job.getOutputVideoPath() == null) {
            return ResponseEntity.notFound().build();
        }
        
        File videoFile = new File(job.getOutputVideoPath());
        if (!videoFile.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = new FileSystemResource(videoFile);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + videoFile.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/jobs")
    @Operation(summary = "List all jobs", 
               description = "Retrieves a paginated list of all karaoke generation jobs")
    public ResponseEntity<Page<KaraokeJobResponse>> listJobs(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<KaraokeJob> jobs = karaokeService.getAllJobs(pageable);
        Page<KaraokeJobResponse> response = jobs.map(this::mapToResponse);
        return ResponseEntity.ok(response);
    }

    private KaraokeJobResponse mapToResponse(KaraokeJob job) {
        String downloadUrl = null;
        if (job.getStatus() == com.karaoke.model.dto.JobStatus.COMPLETED && job.getOutputVideoPath() != null) {
            downloadUrl = "/karaoke/jobs/" + job.getId() + "/download";
        }
        
        return KaraokeJobResponse.builder()
                .id(job.getId())
                .status(job.getStatus())
                .youtubeUrl(job.getYoutubeUrl())
                .progress(job.getProgress())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .downloadUrl(downloadUrl)
                .build();
    }
}
