package com.karaoke.repository;

import com.karaoke.model.entity.KaraokeJob;
import com.karaoke.model.dto.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KaraokeJobRepository extends JpaRepository<KaraokeJob, Long> {
    List<KaraokeJob> findByStatus(JobStatus status);
    Page<KaraokeJob> findAll(Pageable pageable);
}
