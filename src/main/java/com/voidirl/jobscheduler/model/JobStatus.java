package com.voidirl.jobscheduler.model;

public enum JobStatus {
    SCHEDULED,
    TRIGGERED,
    RUNNING,
    COMPLETED,
    FAILED,
    DEAD_LETTERED
}
