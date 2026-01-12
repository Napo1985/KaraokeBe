package com.karaoke.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.karaoke.model.dto.GenerateKaraokeRequest;
import com.karaoke.model.dto.JobStatus;
import com.karaoke.model.entity.KaraokeJob;
import com.karaoke.repository.KaraokeJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class KaraokeControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KaraokeJobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
    }

    @Test
    void generateKaraoke_ShouldCreateJob() throws Exception {
        GenerateKaraokeRequest request = new GenerateKaraokeRequest();
        request.setYoutubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        request.setIncludeBackgroundVocals(false);
        request.setVocalsVolume(0.3);

        mockMvc.perform(post("/karaoke/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Verify job was saved
        assertThat(jobRepository.count()).isEqualTo(1);
        KaraokeJob job = jobRepository.findAll().get(0);
        assertThat(job.getYoutubeUrl()).isEqualTo(request.getYoutubeUrl());
        assertThat(job.getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void getJobStatus_ShouldReturnJob() throws Exception {
        // Create a job directly
        KaraokeJob job = KaraokeJob.builder()
                .status(JobStatus.PROCESSING)
                .youtubeUrl("https://www.youtube.com/watch?v=test")
                .progress(50)
                .options("{}")
                .build();
        job = jobRepository.save(job);

        mockMvc.perform(get("/karaoke/jobs/" + job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(job.getId()))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.progress").value(50));
    }

    @Test
    void getJobStatus_ShouldReturnNotFoundForNonExistentJob() throws Exception {
        mockMvc.perform(get("/karaoke/jobs/999"))
                .andExpect(status().isNotFound());
    }
}
