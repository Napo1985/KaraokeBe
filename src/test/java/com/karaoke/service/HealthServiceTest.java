package com.karaoke.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HealthServiceTest {

    @Autowired
    private HealthService healthService;

    @Test
    void testGetHealthStatus() {
        Map<String, Object> healthStatus = healthService.getHealthStatus();

        assertThat(healthStatus).isNotNull();
        assertThat(healthStatus.get("status")).isEqualTo("UP");
        assertThat(healthStatus.get("service")).isEqualTo("Karaoke Backend API");
        assertThat(healthStatus.containsKey("timestamp")).isTrue();
    }
}
