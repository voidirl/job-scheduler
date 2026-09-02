package com.voidirl.jobscheduler.repository;

import com.voidirl.jobscheduler.model.Job;
import com.voidirl.jobscheduler.model.JobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByStatus(JobStatus status);

    @Query("""
            select j from Job j
            where j.status = :status
              and j.scheduledTime <= :now
              and (j.nextAttemptTime is null or j.nextAttemptTime <= :now)
            order by j.scheduledTime asc
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Job> findDueJobs(@Param("status") JobStatus status, @Param("now") LocalDateTime now);
}
