package com.karaoke.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.karaoke.model.dto.GenerateKaraokeRequest;
import com.karaoke.model.dto.JobStatus;
import com.karaoke.model.entity.KaraokeJob;
import com.karaoke.service.KaraokeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KaraokeController.class)
class KaraokeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KaraokeService karaokeService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void generateKaraoke_ShouldReturnJobId() throws Exception {
        GenerateKaraokeRequest request = new GenerateKaraokeRequest();
        request.setYoutubeUrl("https://www.youtube.com/watch?v=test123");
        request.setIncludeBackgroundVocals(false);
        request.setVocalsVolume(0.3);

        KaraokeJob job = KaraokeJob.builder()
                .id(1L)
                .status(JobStatus.PENDING)
                .youtubeUrl(request.getYoutubeUrl())
                .progress(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(karaokeService.createJob(any(GenerateKaraokeRequest.class))).thenReturn(job);
        doNothing().when(karaokeService).processJob(any(Long.class));

        mockMvc.perform(post("/karaoke/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void generateKaraoke_ShouldReturnBadRequestForInvalidUrl() throws Exception {
        GenerateKaraokeRequest request = new GenerateKaraokeRequest();
        request.setYoutubeUrl("invalid-url");
        request.setIncludeBackgroundVocals(false);
        request.setVocalsVolume(0.3);

        mockMvc.perform(post("/karaoke/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getJobStatus_ShouldReturnJobStatus() throws Exception {
        KaraokeJob job = KaraokeJob.builder()
                .id(1L)
                .status(JobStatus.PROCESSING)
                .youtubeUrl("https://www.youtube.com/watch?v=test123")
                .progress(50)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(karaokeService.getJob(1L)).thenReturn(job);

        mockMvc.perform(get("/karaoke/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.progress").value(50));
    }

    @Test
    void listJobs_ShouldReturnPaginatedJobs() throws Exception {
        KaraokeJob job = KaraokeJob.builder()
                .id(1L)
                .status(JobStatus.COMPLETED)
                .youtubeUrl("https://www.youtube.com/watch?v=test123")
                .progress(100)
                .errorMessage(null)
                .outputVideoPath("path/to/video.mp4")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Page<KaraokeJob> page = new PageImpl<>(Collections.singletonList(job), PageRequest.of(0, 1), 1L);
        when(karaokeService.getAllJobs(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/karaoke/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));
    }
}
