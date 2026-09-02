package com.voidirl.jobscheduler.executor;

import com.voidirl.jobscheduler.model.Job;

public interface JobTask {
    void run(Job job);
}
