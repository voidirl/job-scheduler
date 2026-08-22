package com.voidirl.jobscheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    public Job createJob(JobRequest request){
        Job job = new Job();
        job.setJobName(request.jobName());
        job.setScheduledTime(request.scheduledTime());
        if (request.maxRetries() != null) {
            job.setMaxRetries(request.maxRetries());
        }
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
    public Optional<Job> getJob(Long id){
        return jobRepository.findById(id);
    }
    public Optional<Job> updateJob(Long id, JobRequest request){
        return jobRepository.findById(id).map(job -> {
            job.setJobName(request.jobName());
            job.setScheduledTime(request.scheduledTime());
            if (request.maxRetries() != null) {
                job.setMaxRetries(request.maxRetries());
            }
            return jobRepository.save(job);
        });
    }
    public void deleteJob(Long id){
        if (jobRepository.existsById(id)) {
            jobRepository.deleteById(id);
        }
    }
}
