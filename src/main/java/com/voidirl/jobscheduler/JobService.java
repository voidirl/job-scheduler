package com.voidirl.jobscheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    public Job createJob(String jobName, LocalDateTime scheduledTime){
        Job job = new Job();
        job.setJobName(jobName);
        job.setScheduledTime(scheduledTime);
        job.setStatus(JobStatus.SCHEDULED);
        job.setCreatedAt(LocalDateTime.now());
        return jobRepository.save(job);
    }
    public List<Job> getAllJobs(){
        return jobRepository.findAll();
    }
    public List<Job> getJobsByStatus(JobStatus status){
        return jobRepository.findByStatus(status);
    }
}
