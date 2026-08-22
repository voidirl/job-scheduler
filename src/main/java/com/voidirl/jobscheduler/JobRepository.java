package com.voidirl.jobscheduler;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatus(JobStatus status);

    List<Job> findByStatusAndScheduledTimeLessThanEqualOrderByScheduledTimeAsc(JobStatus status, LocalDateTime time);
}
