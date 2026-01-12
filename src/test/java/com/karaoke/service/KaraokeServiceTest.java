package com.karaoke.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.karaoke.model.dto.GenerateKaraokeRequest;
import com.karaoke.model.dto.JobStatus;
import com.karaoke.model.entity.KaraokeJob;
import com.karaoke.repository.KaraokeJobRepository;
import com.karaoke.service.audio.AudioSeparationService;
import com.karaoke.service.lyrics.LyricsService;
import com.karaoke.service.video.KaraokeVideoGeneratorService;
import com.karaoke.service.video.VideoDownloadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KaraokeServiceTest {

    @Mock
    private KaraokeJobRepository jobRepository;

    @Mock
    private VideoDownloadService videoDownloadService;

    @Mock
    private AudioSeparationService audioSeparationService;

    @Mock
    private LyricsService lyricsService;

    @Mock
    private KaraokeVideoGeneratorService karaokeVideoGeneratorService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KaraokeService karaokeService;

    private GenerateKaraokeRequest request;
    private KaraokeJob job;

    @BeforeEach
    void setUp() {
        request = new GenerateKaraokeRequest();
        request.setYoutubeUrl("https://www.youtube.com/watch?v=test123");
        request.setIncludeBackgroundVocals(false);
        request.setVocalsVolume(0.3);

        job = KaraokeJob.builder()
                .id(1L)
                .status(JobStatus.PENDING)
                .youtubeUrl(request.getYoutubeUrl())
                .progress(0)
                .options("{}")
                .build();
    }

    @Test
    void createJob_ShouldSaveAndReturnJob() throws Exception {
        when(jobRepository.save(any(KaraokeJob.class))).thenReturn(job);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        KaraokeJob result = karaokeService.createJob(request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(result.getYoutubeUrl()).isEqualTo(request.getYoutubeUrl());
        verify(jobRepository, times(1)).save(any(KaraokeJob.class));
    }

    @Test
    void getJob_ShouldReturnJobWhenExists() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        KaraokeJob result = karaokeService.getJob(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getJob_ShouldThrowExceptionWhenNotFound() {
        when(jobRepository.findById(1L)).thenReturn(Optional.empty());

        try {
            karaokeService.getJob(1L);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("not found");
        }
    }
}
