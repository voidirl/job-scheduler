package com.voidirl.jobscheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class JobExecutionEngineIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @MockitoBean
    private JobTask jobTask;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        jobRepository.deleteAll();
    }

    private Job seedJob(String name, LocalDateTime scheduledTime, int maxRetries) {
        Job job = new Job();
        job.setJobName(name);
        job.setScheduledTime(scheduledTime);
        job.setStatus(JobStatus.SCHEDULED);
        job.setMaxRetries(maxRetries);
        job.setCreatedAt(LocalDateTime.now());
        return jobRepository.saveAndFlush(job);
    }

    private Job awaitStatus(long id, JobStatus expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            var job = jobRepository.findById(id);
            if (job.isPresent() && job.get().getStatus() == expected) {
                return job.get();
            }
            Thread.sleep(250);
        }
        var last = jobRepository.findById(id);
        throw new AssertionError("Job " + id + " did not reach " + expected
                + " in time; final status = " + last.map(j -> j.getStatus().name()).orElse("MISSING"));
    }

    @Test
    void executesDueJobAndMarksCompleted() throws Exception {
        Job seeded = seedJob("due-job", LocalDateTime.now().minusSeconds(1), 3);
        Job job = awaitStatus(seeded.getId(), JobStatus.COMPLETED);
        assertEquals(0, job.getRetryCount());
    }

    @Test
    void retriesFailingJobUntilMaxRetriesThenMarksFailed() throws Exception {
        doThrow(new RuntimeException("simulated failure")).when(jobTask).run(any());
        Job seeded = seedJob("failing-job", LocalDateTime.now().minusSeconds(1), 1);
        Job job = awaitStatus(seeded.getId(), JobStatus.FAILED);
        assertEquals(2, job.getRetryCount());
    }

    @Test
    void leavesFutureJobsUntouched() throws Exception {
        Job seeded = seedJob("future-job", LocalDateTime.now().plusHours(1), 3);
        Thread.sleep(3500);
        assertEquals(JobStatus.SCHEDULED, jobRepository.findById(seeded.getId()).orElseThrow().getStatus());
    }
}
