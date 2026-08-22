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

    private final JobRepository jobRepository;
    private final JobTask jobTask;

    public JobExecutionEngine(JobRepository jobRepository, JobTask jobTask) {
        this.jobRepository = jobRepository;
        this.jobTask = jobTask;
    }

    @Scheduled(fixedDelay = 2000)
    public void runDueJobs() {
        List<Job> dueJobs = jobRepository.findByStatusAndScheduledTimeLessThanEqualOrderByScheduledTimeAsc(
                JobStatus.SCHEDULED, LocalDateTime.now());
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
            job.setStatus(retriesLeft ? JobStatus.SCHEDULED : JobStatus.FAILED);
            log.warn("Job {} attempt {} failed: {} ({})", job.getId(), attempts, e.getMessage(),
                    retriesLeft ? "will retry" : "retries exhausted");
        }
        jobRepository.save(job);
    }
}
