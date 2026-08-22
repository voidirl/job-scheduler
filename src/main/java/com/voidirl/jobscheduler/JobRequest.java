package com.voidirl.jobscheduler;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record JobRequest(
        @NotBlank String jobName,
        @NotNull @Future LocalDateTime scheduledTime,
        @Min(0) Integer maxRetries
) {}
