package com.karaoke.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class GenerateKaraokeRequest {

    @NotBlank(message = "YouTube URL is required")
    @Pattern(regexp = "^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/.+", 
             message = "Invalid YouTube URL")
    private String youtubeUrl;

    @NotNull(message = "includeBackgroundVocals cannot be null")
    private Boolean includeBackgroundVocals = false;

    @NotNull(message = "vocalsVolume cannot be null")
    private Double vocalsVolume = 0.3; // Default to 30% volume for background vocals
}
