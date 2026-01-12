package com.karaoke.model.entity;

import com.karaoke.model.dto.JobStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "karaoke_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KaraokeJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(nullable = false, length = 2048)
    private String youtubeUrl;

    @Column(nullable = false)
    private Integer progress;

    @Column(length = 4096)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 2048)
    private String outputVideoPath;

    @Column(columnDefinition = "TEXT")
    private String options; // JSON string for job options

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = JobStatus.PENDING;
        }
        if (progress == null) {
            progress = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
