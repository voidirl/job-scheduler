package com.voidirl.jobscheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class JobExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionEngine.class);

    private static final long BACKOFF_BASE_SECONDS = 5;
    private static final long BACKOFF_MAX_SECONDS = 300;

    private final JobRepository jobRepository;
    private final JobTask jobTask;

    public JobExecutionEngine(JobRepository jobRepository, JobTask jobTask) {
        this.jobRepository = jobRepository;
        this.jobTask = jobTask;
    }

    @Scheduled(fixedDelay = 2000)
    public void runDueJobs() {
        List<Job> dueJobs = jobRepository.findDueJobs(JobStatus.SCHEDULED, LocalDateTime.now());
        for (Job job : dueJobs) {
            execute(job);
        }
    }

    void execute(Job job) {
        job.setStatus(JobStatus.TRIGGERED);
        jobRepository.save(job);
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
                job.setStatus(JobStatus.FAILED);
                log.warn("Job {} attempt {} failed: {} (retries exhausted)", job.getId(), attempts, e.getMessage());
            }
        }
        jobRepository.save(job);
    }
}
