package com.karaoke.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KaraokeJobResponse {
    private Long id;
    private JobStatus status;
    private String youtubeUrl;
    private Integer progress;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String downloadUrl; // URL to download the video when completed
}
