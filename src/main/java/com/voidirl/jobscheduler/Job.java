package com.voidirl.jobscheduler;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String jobName;

    @Enumerated(EnumType.STRING)
    private JobStatus status;
    private LocalDateTime scheduledTime;

    private int retryCount = 0;
    private int maxRetries = 3;

    private LocalDateTime createdAt;

    public Long getId(){return id;}
    public void setId(Long id){this.id = id;}

    public String getJobName(){return jobName;}
    public void setJobName(String jobName){this.jobName = jobName;}

    public JobStatus getStatus(){return status;}
    public void setStatus(JobStatus status){this.status = status;}

    public LocalDateTime getScheduledTime(){return scheduledTime;}
    public void setScheduledTime(LocalDateTime scheduledTime){this.scheduledTime = scheduledTime;}

    public int getRetryCount(){return retryCount;}
    public void setRetryCount(int retryCount){this.retryCount = retryCount;}

    public int getMaxRetries(){return maxRetries;}
    public void setMaxRetries(int maxRetries){this.maxRetries = maxRetries;}

    public LocalDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt = createdAt;
    }
}
