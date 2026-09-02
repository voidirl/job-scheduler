package com.voidirl.jobscheduler;

import com.voidirl.jobscheduler.model.Job;
import com.voidirl.jobscheduler.model.JobStatus;
import com.voidirl.jobscheduler.repository.JobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JobExecutionEngineIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    private final RestClient restClient = RestClient.create();

    @LocalServerPort
    private int port;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        jobRepository.deleteAll();
        TestCallbackController.HITS.set(0);
        TestCallbackController.MODE.set(TestCallbackController.Mode.OK);
    }

    private Job seedJob(String name, LocalDateTime scheduledTime, int maxRetries, String callbackUrl) {
        Job job = new Job();
        job.setJobName(name);
        job.setScheduledTime(scheduledTime);
        job.setStatus(JobStatus.SCHEDULED);
        job.setMaxRetries(maxRetries);
        job.setCallbackUrl(callbackUrl);
        job.setCreatedAt(LocalDateTime.now());
        return jobRepository.saveAndFlush(job);
    }

    private Job awaitStatus(long id, JobStatus expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
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

    private void awaitHits(int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline && TestCallbackController.HITS.get() < expected) {
            Thread.sleep(250);
        }
        assertEquals(expected, TestCallbackController.HITS.get());
    }

    private void awaitAllCompleted(int expectedCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            List<Job> all = jobRepository.findAll();
            if (all.size() == expectedCount
                    && all.stream().allMatch(j -> j.getStatus() == JobStatus.COMPLETED)) {
                return;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Jobs did not all complete; final statuses: "
                + jobRepository.findAll().stream().map(j -> j.getId() + "=" + j.getStatus()).toList());
    }

    private String callbackUrl() {
        return "http://localhost:" + port + "/test-callback";
    }

    @Test
    void executesDueJobAndMarksCompleted() throws Exception {
        Job seeded = seedJob("due-job", LocalDateTime.now().minusSeconds(1), 3, null);
        Job job = awaitStatus(seeded.getId(), JobStatus.COMPLETED);
        assertEquals(0, job.getRetryCount());
    }

    @Test
    void retriesFailingJobUntilMaxRetriesThenDeadLetters() throws Exception {
        TestCallbackController.MODE.set(TestCallbackController.Mode.FAIL);
        Job seeded = seedJob("failing-job", LocalDateTime.now().minusSeconds(1), 1, callbackUrl());
        Job job = awaitStatus(seeded.getId(), JobStatus.DEAD_LETTERED);
        assertEquals(2, job.getRetryCount());
        assertEquals(2, TestCallbackController.HITS.get());
    }

    @Test
    void leavesFutureJobsUntouched() throws Exception {
        Job seeded = seedJob("future-job", LocalDateTime.now().plusHours(1), 3, null);
        Thread.sleep(3500);
        assertEquals(JobStatus.SCHEDULED, jobRepository.findById(seeded.getId()).orElseThrow().getStatus());
    }

    @Test
    void deliversCallbackToHttpEndpointAndMarksCompleted() throws Exception {
        Job seeded = seedJob("callback-job", LocalDateTime.now().minusSeconds(1), 3, callbackUrl());
        awaitStatus(seeded.getId(), JobStatus.COMPLETED);
        awaitHits(1);
    }

    @Test
    void failingCallbackSchedulesBackoffRetryThenSucceedsOnSecondAttempt() throws Exception {
        TestCallbackController.MODE.set(TestCallbackController.Mode.FAIL);
        Job seeded = seedJob("flaky-endpoint-job", LocalDateTime.now().minusSeconds(1), 3, callbackUrl());

        awaitHits(1);
        TestCallbackController.MODE.set(TestCallbackController.Mode.OK);

        Job job = awaitStatus(seeded.getId(), JobStatus.COMPLETED);
        assertEquals(2, TestCallbackController.HITS.get());
        assertEquals(1, job.getRetryCount());
    }

    @Test
    void executesManyDueJobsConcurrently() throws Exception {
        for (int i = 0; i < 8; i++) {
            seedJob("bulk-job-" + i, LocalDateTime.now().minusSeconds(1), 3, null);
        }
        awaitAllCompleted(8);
    }

    @Test
    void deliversEachDueCallbackExactlyOnceWhenRunningConcurrently() throws Exception {
        for (int i = 0; i < 8; i++) {
            seedJob("bulk-callback-" + i, LocalDateTime.now().minusSeconds(1), 3, callbackUrl());
        }
        awaitAllCompleted(8);
        awaitHits(8);
    }

    @Test
    void deadLetterEndpointListsPermanentlyFailedJobs() throws Exception {
        TestCallbackController.MODE.set(TestCallbackController.Mode.FAIL);
        Job seeded = seedJob("dead-letter-job", LocalDateTime.now().minusSeconds(1), 1, callbackUrl());
        awaitStatus(seeded.getId(), JobStatus.DEAD_LETTERED);

        Job[] deadLetters = restClient.get()
                .uri("http://localhost:" + port + "/api/jobs/dead-letter")
                .retrieve()
                .body(Job[].class);
        assertNotNull(deadLetters);
        assertEquals(1, deadLetters.length);
        assertEquals(seeded.getId(), deadLetters[0].getId());
    }
}
