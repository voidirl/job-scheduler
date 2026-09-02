package com.voidirl.jobscheduler.executor;

import com.voidirl.jobscheduler.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class HttpCallbackJobTask implements JobTask {

    private static final Logger log = LoggerFactory.getLogger(HttpCallbackJobTask.class);

    private final RestClient restClient = RestClient.create();

    @Override
    public void run(Job job) {
        if (job.getCallbackUrl() == null || job.getCallbackUrl().isBlank()) {
            log.info("Executing job {}: {} (no callback url, simulating)", job.getId(), job.getJobName());
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while executing job " + job.getId(), e);
            }
            return;
        }
        Map<String, Object> payload = Map.of(
                "jobId", job.getId(),
                "jobName", job.getJobName(),
                "attempt", job.getRetryCount() + 1);
        restClient.post()
                .uri(job.getCallbackUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
        log.info("Job {} delivered callback to {}", job.getId(), job.getCallbackUrl());
    }
}
