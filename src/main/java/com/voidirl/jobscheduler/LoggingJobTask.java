package com.voidirl.jobscheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingJobTask implements JobTask {

    private static final Logger log = LoggerFactory.getLogger(LoggingJobTask.class);

    @Override
    public void run(Job job) {
        log.info("Executing job {}: {}", job.getId(), job.getJobName());
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while executing job " + job.getId(), e);
        }
    }
}
