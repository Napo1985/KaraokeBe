package com.karaoke.integration;

import com.karaoke.KaraokeBeApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = KaraokeBeApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testHealthEndpointIntegration() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Karaoke Backend API"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
