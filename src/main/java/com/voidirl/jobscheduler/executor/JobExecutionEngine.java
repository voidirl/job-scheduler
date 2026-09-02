package com.voidirl.jobscheduler.executor;

import com.voidirl.jobscheduler.model.Job;
import com.voidirl.jobscheduler.model.JobStatus;
import com.voidirl.jobscheduler.repository.JobRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class JobExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionEngine.class);

    private static final long BACKOFF_BASE_SECONDS = 5;
    private static final long BACKOFF_MAX_SECONDS = 300;

    private final JobRepository jobRepository;
    private final JobTask jobTask;
    private final ExecutorService executor;

    public JobExecutionEngine(JobRepository jobRepository, JobTask jobTask,
                              @Value("${jobs.executor.pool-size:4}") int poolSize) {
        this.jobRepository = jobRepository;
        this.jobTask = jobTask;
        this.executor = Executors.newFixedThreadPool(poolSize);
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void runDueJobs() {
        List<Job> dueJobs = jobRepository.findDueJobs(JobStatus.SCHEDULED, LocalDateTime.now());
        for (Job job : dueJobs) {
            job.setStatus(JobStatus.TRIGGERED);
            jobRepository.save(job);
            executor.execute(() -> execute(job));
        }
    }

    void execute(Job job) {
        job.setStatus(JobStatus.RUNNING);
        jobRepository.save(job);

        try {
            jobTask.run(job);
            job.setStatus(JobStatus.COMPLETED);
            log.info("Job {} completed", job.getId());
        } catch (Exception e) {
            int attempts = job.getRetryCount() + 1;
            job.setRetryCount(attempts);
            boolean retriesLeft = attempts <= job.getMaxRetries();
            if (retriesLeft) {
                long delaySeconds = Math.min(
                        BACKOFF_BASE_SECONDS << Math.min(attempts - 1, 6),
                        BACKOFF_MAX_SECONDS);
                job.setNextAttemptTime(LocalDateTime.now().plusSeconds(delaySeconds));
                job.setStatus(JobStatus.SCHEDULED);
                log.warn("Job {} attempt {} failed: {} (retry in {}s)", job.getId(), attempts, e.getMessage(), delaySeconds);
            } else {
                job.setStatus(JobStatus.DEAD_LETTERED);
                log.warn("Job {} attempt {} failed: {} (retries exhausted, dead-lettered)", job.getId(), attempts, e.getMessage());
            }
        }
        jobRepository.save(job);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}