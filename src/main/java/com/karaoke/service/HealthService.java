package com.karaoke.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class HealthService {

    @Cacheable(value = "health", key = "'status'")
    public Map<String, Object> getHealthStatus() {
        return Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "service", "Karaoke Backend API"
        );
    }
}
